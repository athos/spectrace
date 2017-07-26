(ns spectrace.core
  (:require [clojure.spec.alpha :as s]
            #?(:cljs [cljs.compiler :as comp])))

(def ^:dynamic *eval-fn*
  #?(:clj eval :cljs nil))

(defn- eval* [x]
  (assert *eval-fn* "spectrace.core/*eval-fn* must be bound")
  (*eval-fn* x))

(def ^:private ^:dynamic *problem-indexes*)

(defn- next-problem [{:keys [::s/problems]} trail]
  (let [index (get *problem-indexes* trail 0)]
    (when (< index (count problems))
      (set! *problem-indexes* (assoc *problem-indexes* trail (inc index)))
      (nth problems index))))

(defmulti step (fn [state] (first (:spec state))))
(defmethod step :default [{:keys [spec]}]
  (throw
    (ex-info (str "spec macro " spec
                  " must have its own method implementation for spectrace.core/step")
             {:spec spec})))

(defmethod step `s/spec [state]
  (update state :spec second))

(defn- interleave-specs [specs {:keys [val trail] :as state}]
  (loop [i 0, [spec & specs] specs, val val, snapshots [val]]
    (assert (not (nil? spec)))
    (let [evaled-spec (eval* spec)
          conformed (s/conform evaled-spec val)]
      (if (s/invalid? conformed)
        (let [ed (s/explain-data evaled-spec val)
              trail (conj trail i)
              {:keys [path in]} (next-problem ed trail)]
          (assoc state
                 :spec spec :path path :val val :in in
                 :trail trail :snapshots snapshots))
        (recur (inc i) specs conformed (conj snapshots conformed))))))

(defmethod step `s/and [state]
  (interleave-specs (rest (:spec state)) state))

(defn- step-forward [{:keys [spec path] :as state}]
  (let [[segment & path] path]
    (let [spec' (some (fn [[tag spec]] (and (= tag segment) spec))
                      (partition 2 (rest spec)))]
      (-> state
          (assoc :spec spec' :path path)
          (update :trail conj segment)))))

(defmethod step `s/or [state]
  (step-forward state))

(defmethod step `s/nilable [{:keys [path] :as state}]
  (let [[segment & path] path
        state (assoc state :path path)]
    (case segment
      ::s/pred (update state :spec second)
      ::s/nil (assoc state :spec 'nil?))))

(defmethod step `s/tuple [{:keys [spec path val in pred] :as state}]
  (if (empty? path)
    (do (assert (or (= pred 'vector?)
                    (s/valid? (s/cat := `#{=} :count `#{(count ~'%)}
                                     :n integer?)
                              pred)))
        (assoc state :spec pred))
    (let [[segment & path] path
          [key & in] in]
      (-> state
          (assoc :spec (nth (rest spec) segment)
                 :path path
                 :val (nth val key)
                 :in in)
          (update :trail conj segment)))))

(defn- step-for-every [{:keys [path val in pred] :as state}]
  (if (and (empty? path) (= pred `coll?))
    (assoc state :spec pred)
    (let [[key & in] in]
      (-> state
          (update :spec second)
          (assoc :val (nth (seq val) key))
          (assoc :in in)))))

(defmethod step `s/every [state]
  (step-for-every state))

(defmethod step `s/coll-of [state]
  (step-for-every state))

(defn- step-for-every-kv [{:keys [spec path val in] :as state}]
  (let [[segment & path] path
        [key1 key2 & in] in
        pred-key (get #{0 1} segment)
        specs (take 2 (rest spec))]
    (-> state
        (assoc :spec (nth specs pred-key) :path path
               :val (-> val (find key1) (nth key2)) :in in)
        (update :trail conj key2))))

(defmethod step `s/map-of [{:keys [path pred] :as state}]
  (if (and (empty? path) (= pred `map?))
    (assoc state :spec pred)
    (step-for-every-kv state)))

(defmethod step `s/every-kv [{:keys [path pred] :as state}]
  (if (and (empty? path) (= pred `coll?))
    (assoc state :spec pred)
    (step-for-every-kv state)))

(defn- possible-keys [[& {:as args}]]
  (letfn [(walk [ret maybe-key]
            (if (keyword? maybe-key)
              (conj ret maybe-key)
              (collect-keys ret (rest maybe-key))))
          (collect-keys [ret keys]
            (reduce walk ret keys))]
    (-> {}
        (into (map (fn [k] [k k]))
              (collect-keys (set (:opt args)) (:req args)))
        (into (map (fn [k] [(keyword (name k)) k]))
              (collect-keys (set (:opt-un args)) (:req-un args))))))

(defn- step-for-keys [{:keys [spec path val pred] :as state}
                      & {:keys [val-fn]}]
  (let [keys (possible-keys (rest spec))]
    (if (empty? path)
      (if (= pred 'map?)
        (assoc state :spec pred)
        (let [fn? (s/cat :fn `#{fn} :args (s/tuple '#{%})
                         :body (s/and seq?
                                      (s/cat :f `#{contains?} :arg '#{%}
                                             :key #(contains? keys %))))]
          (assert (s/valid? fn? pred))
          (assoc state :spec pred)))
      (let [[segment & path] path
            [key & in] (:in state)]
        (-> state
            (assoc :spec (get keys segment) :path path
                   :val (cond-> val val-fn (val-fn key)) :in in)
            (update :trail conj segment))))))

(defmethod step `s/keys [state]
  (step-for-keys state :val-fn get))

;; Add this after CLJ-2143 is fixed
#_(defmethod step `s/keys* [state]
  (letfn [(get-key [[& {:as keys}] key]
            (get keys key))]
    (step-for-keys state get-key)))

(defmethod step `s/merge [{:keys [spec val trail] :as state}]
  (loop [i 0, [spec & specs] (rest spec)]
    (assert (not (nil? spec)))
    (if-let [ed (s/explain-data (eval* spec) val)]
      (let [trail (conj trail i)]
        (if-let [{:keys [path in]} (next-problem ed trail)]
          (assoc state :spec spec :path path :in in :trail trail)
          (recur (inc i) specs)))
      (recur (inc i) specs))))

(def ^:private regex-ops
  `#{s/cat s/& s/alt s/? s/* s/+})

(defn- regex-postprocess [{:keys [spec path val in pred reason] :as state}]
  (if (or (and (seq? spec) (contains? regex-ops (first spec)))
          (empty? in))
    (if (and (empty? path) (= reason "Insufficient input"))
      (assoc state :spec pred)
      state)
    (let [[key & in] in]
      (assoc state :val (nth val key) :in in))))

(defn- with-regex-processing [{:keys [path reason] :as state} f]
  (when-not (and (empty? path) (= reason "Extra input"))
    (f state)))

(defmethod step `s/cat [{:keys [path pred] :as state}]
  (with-regex-processing state
    #(regex-postprocess (step-forward %))))

(defmethod step `s/& [state]
  (interleave-specs (rest (:spec state)) state))

(defmethod step `s/alt [state]
  (with-regex-processing state
    #(regex-postprocess (step-forward %))))

(defn- step-for-rep [{:keys [val in] :as state}]
  (-> state
      (update :spec second)
      regex-postprocess))

(defmethod step `s/? [state]
  (with-regex-processing state step-for-rep))

(defmethod step `s/* [state]
  (step-for-rep state))

(defmethod step `s/+ [state]
  (step-for-rep state))

(defmethod step `s/fspec [{:keys [path pred] :as state}]
  (if (empty? path)
    (assoc state :spec pred)
    (step-forward state)))

(defn- method-of [multi-name key]
  #?(:clj (let [maybe-multi (resolve multi-name)]
            (get-method @maybe-multi key))
     :cljs (let [;; I'm not sure this is the right way
                 ;; to resolve a symbol in CLJS
                 multi (js/eval (str (comp/munge multi-name)))]
             (get-method multi key))))

(defmethod step `s/multi-spec [{:keys [spec path] :as state}]
  (let [[segment & path] path
        multi-name (second spec)]
    (let [method (method-of multi-name segment)]
      (-> state
          (assoc :spec (method (:val state)) :path path)
          (update :trail conj segment)))))

(defmethod step `s/nonconforming [state]
  (update state :spec second))

(defn- normalize [{:keys [spec path val in trail snapshots]}]
  (let [spec' (if (or (keyword? spec) (s/spec? spec) (s/regex? spec))
                (s/form spec)
                spec)
        state (cond-> {:spec spec'
                       :path (vec path)
                       :val val
                       :in (vec in)
                       :trail (vec trail)}
                snapshots (assoc :snapshots snapshots))]
    (if (keyword? spec)
      (assoc state :spec-name spec)
      (dissoc state :spec-name))))

(defn- trace [{:keys [path in val pred reason] :as problem} spec value]
  (let [state (normalize {:spec spec :path path :val value :in in})]
    (loop [{:keys [spec] :as state} state,
           ret [state]]
      (if (= spec pred)
        ret
        (if-let [state' (some-> (step (cond-> (assoc state :pred pred)
                                        reason (assoc :reason reason)))
                                normalize)]
          (recur (dissoc state' :snapshots) (conj ret state'))
          ret)))))

(defn traces [{:keys [::s/spec ::s/value] :as ed}]
  (binding [*problem-indexes* {}]
    (mapv #(trace % spec value) (::s/problems ed))))
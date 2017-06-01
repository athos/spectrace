(ns spectrace.trace
  (:require [clojure.spec.alpha :as s]
            [spectrace.specs :as specs]))

(s/def ::spec any?)
(s/def ::path (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::val any?)
(s/def ::in (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::pred any?)
(s/def ::spec-name keyword?)

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in ::pred]
          :opt-un [::spec-name]))

(s/def ::succ (s/fspec :args (s/cat :arg ::state) :ret any?))
(s/def ::fail (s/fspec :args (s/cat) :ret any?))

(s/fdef step*
  :args (s/cat :state ::state
               :succ  ::succ
               :fail  ::fail)
  :ret ::state)

(defmulti step* (fn [state succ fail] (first (:spec state))))
(defmethod step* :default [{:keys [spec]} _ _]
  (throw
    (ex-info (str "spec macro " spec
                  " must have its own method implementation for spectrace.trace/step*")
             {:spec spec})))

(defn- with-cont [succ fail ret]
  (if ret
    (succ ret fail)
    (fail)))

(defn- choose-spec [specs state succ fail]
  (letfn [(rec [specs]
            (if (empty? specs)
              (fail)
              (succ (assoc state :spec (first specs))
                    #(rec (rest specs)))))]
    (rec specs)))

(defmethod step* `s/and [state succ fail]
  (let [specs (rest (:spec state))]
    (choose-spec specs state succ fail)))

(defn- step-by-key [{:keys [spec path val in]} succ fail & {:keys [val-fn]}]
  (with-cont succ fail
    (let [[segment & path] path, [key & in] in]
      (when-let [spec' (some (fn [[tag spec]] (and (= tag segment) spec))
                             (partition 2 (rest spec)))]
        {:spec spec' :path path :in in
         :val (cond-> val val-fn (val-fn key))}))))

(defmethod step* `s/or [state succ fail]
  (step-by-key state succ fail))

(defmethod step* `s/nilable [{:keys [path] :as state} succ fail]
  (with-cont succ fail
    (let [[segment & path] path
          state (assoc state :path path)]
      (case segment
        ::s/pred (update state :spec second)
        ::s/nil (assoc state :spec 'nil?)
        nil))))

(defmethod step* `s/tuple [{:keys [spec path val in] :as state} succ fail]
  (with-cont succ fail
    (let [[segment & path] path
          [key & in] in]
      (-> state
          (assoc :spec (nth (rest spec) segment)
                 :path path
                 :val (nth val key)
                 :in in)))))

(defn- step-for-every [{:keys [val in] :as state} succ fail]
  (with-cont succ fail
    (let [[key & in] in]
      (when (and (vector? val) (> (count val) key))
        (-> state
            (update :spec second)
            (assoc :val (nth val key))
            (assoc :in in))))))

(defmethod step* `s/every [state succ fail]
  (step-for-every state succ fail))

(defmethod step* `s/coll-of [state succ fail]
  (step-for-every state succ fail))

(defn- step-for-every-kv [{:keys [spec path val in] :as state} succ fail]
  (with-cont succ fail
    (let [[segment & path] path
          [key1 key2 & in] in
          pred-key (get #{0 1} segment)
          specs (take 2 (rest spec))]
      (when (and pred-key
                 (< pred-key (count specs))
                 (map? val)
                 (contains? val key1))
        {:spec (nth specs pred-key)
         :path path
         :val (-> val (find key1) (nth key2))
         :in in}))))

(defmethod step* `s/map-of [state succ fail]
  (step-for-every-kv state succ fail))

(defmethod step* `s/every-kv [state succ fail]
  (step-for-every-kv state succ fail))

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

(defn- step-for-keys [{:keys [spec path val pred] :as state} succ fail
                      & {:keys [val-fn]}]
  (with-cont succ fail
    (let [keys (possible-keys (rest spec))]
      (if (empty? path)
        (let [fn? (s/cat :fn `#{fn} :args (s/tuple '#{%})
                         :body (s/and seq?
                                      (s/cat :f `#{contains?} :arg '#{%}
                                             :key #(contains? keys %))))]
          (when (s/valid? fn? pred)
            (assoc state :spec pred)))
        (let [[segment & path] path
              [key & in] (:in state)]
          (when (and (contains? keys segment)
                     (map? val)
                     (contains? val key))
            {:spec (get keys segment)
             :path path
             :val (cond-> val val-fn (val-fn key))
             :in in}))))))

(defmethod step* `s/keys [state succ fail]
  (step-for-keys state succ fail :val-fn get))

;; Add this after CLJ-2143 is fixed
#_(defmethod step* `s/keys* [state succ fail]
  (letfn [(get-key [[& {:as keys}] key]
            (get keys key))]
    (step-for-keys state succ fail get-key)))

(defmethod step* `s/merge [state succ fail]
  (choose-spec (rest (:spec state)) state succ fail))

(defmethod step* `s/cat [state succ fail]
  (step-by-key state succ fail :val-fn nth))

(defmethod step* `s/& [state succ fail]
  (choose-spec (rest (:spec state)) state succ fail))

(defmethod step* `s/alt [state succ fail]
  (step-by-key state succ fail :val-fn nth))

(defn- step-for-rep [{:keys [val in] :as state} succ fail]
  (let [[key & in] in]
    (-> state
        (update :spec second)
        (assoc :val (nth val key) :in in)
        (succ fail))))

(defmethod step* `s/? [state succ fail]
  (step-for-rep state succ fail))

(defmethod step* `s/* [state succ fail]
  (step-for-rep state succ fail))

(defmethod step* `s/+ [state succ fail]
  (step-for-rep state succ fail))

(defn- step [{:keys [spec] :as state} succ fail]
  (if (or (set? spec) (symbol? spec) (keyword? spec))
    (succ state fail)
    (step* state succ fail)))

(defn- normalize [{:keys [spec path val in]}]
  (let [spec' (if (or (keyword? spec) (s/spec? spec) (s/regex? spec))
                (s/form spec)
                spec)
        state {:spec spec'
               :path (vec path)
               :val val
               :in (vec in)}]
    (if (keyword? spec)
      (assoc state :spec-name spec)
      (dissoc state :spec-name))))

(defn trace [{:keys [path in val pred] :as problem} spec value]
  (letfn [(rec [{:keys [spec] :as state} fail ret]
            (cond (= spec pred)
                  ret

                  ;; immature failure condition
                  (symbol? spec)
                  fail

                  :else
                  (step (assoc state :pred pred)
                        (fn [state' fail]
                          (let [state' (normalize state')]
                            #(rec state' fail (conj ret state'))))
                        (fn [] fail))))]
    (let [state (normalize {:spec spec :path path :val value :in in})]
      (trampoline rec state (constantly nil) [state]))))

(defn traces [{:keys [::s/spec ::s/value] :as ed}]
  (mapv #(trace % spec value) (::s/problems ed)))

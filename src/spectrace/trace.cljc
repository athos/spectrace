(ns spectrace.trace
  (:require [clojure.spec :as s]
            [spectrace.specs :as specs]))

(s/def ::spec any?)
(s/def ::path (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::val any?)
(s/def ::in (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::pred any?)
(s/def ::spec-name keyword?)
(s/def ::skip? boolean?)

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in ::pred]
          :opt-un [::spec-name ::skip?]))

(s/def ::succ (s/fspec :args (s/cat :arg ::state) :ret any?))
(s/def ::fail (s/fspec :args (s/cat) :ret any?))

(s/fdef step*
  :args (s/cat :state ::state
               :succ  ::succ
               :fail  ::fail)
  :ret ::state)

(defmulti step* (fn [state succ fail] (get-in state [:spec :s])))
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
  (let [specs (get-in state [:spec :args])]
    (choose-spec specs state succ fail)))

(defn- step-by-key [{:keys [spec path val in]} succ fail & {:keys [val-fn]}]
  (with-cont succ fail
    (let [[segment & path] path, [key & in] in]
      (when-let [spec' (some #(and (= (:tag %) segment) (:spec %))
                             (:args spec))]
        {:spec spec' :path path :in in
         :val (cond-> val val-fn #(val-fn % key))}))))

(defmethod step* `s/or [state succ fail]
  (step-by-key state succ fail))

(defmethod step* `s/nilable [{:keys [spec] :as state} succ fail]
  (with-cont succ fail
    (-> state
        (assoc :spec (get-in spec [:args :spec]))
        (update :path rest))))

(defmethod step* `s/tuple [{:keys [spec path val] :as state} succ fail]
  (with-cont succ fail
    (let [[segment & path] path]
      (-> state
          (assoc :spec (nth (:args spec) segment)
                 :path path
                 :val (nth val segment))
          (update :in rest)))))

(defn- step-for-every [{:keys [val in] :as state} succ fail]
  (with-cont succ fail
    (let [[key & in] in]
      (when (and (vector? val) (> (count val) key))
        (-> state
            (update :spec get-in [:args :spec])
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
          pred-key (case segment 0 :kpred 1 :vpred nil)]
      (when (and pred-key
                 (contains? (:args spec) pred-key)
                 (map? val)
                 (contains? val key1))
        {:spec (get-in spec [:args pred-key])
         :path path
         :val (-> val (find key1) (nth key2))
         :in in}))))

(defmethod step* `s/map-of [state succ fail]
  (step-for-every-kv state succ fail))

(defmethod step* `s/every-kv [state succ fail]
  (step-for-every-kv state succ fail))

(defn- possible-keys [{:keys [args]}]
  (letfn [(walk [ret [kind arg]]
            (case kind
              :key (conj ret arg)
              (:and :or) (collect-keys ret (:keys arg))))
          (collect-keys [ret keys]
            (reduce walk ret keys))]
    (-> {}
        (into (map (fn [k] [k k]))
              (collect-keys (set (:opt args)) (:req args)))
        (into (map (fn [k] [(keyword (name k)) k]))
              (collect-keys (set (:opt-un args)) (:req-un args))))))

(defn- step-for-keys [{:keys [spec path val in]} succ fail val-fn]
  (with-cont succ fail
    (let [[segment & path] path
          [key & in] in
          keys (possible-keys spec)]
      (when (and (contains? keys segment)
                 (map? val)
                 (contains? val key))
        {:spec (get keys segment)
         :path path
         :val (val-fn val key)
         :in in}))))

(defmethod step* `s/keys [state succ fail]
  (step-for-keys state succ fail get))

;; Add this after CLJ-2143 is fixed
#_(defmethod step* `s/keys* [state succ fail]
  (letfn [(get-key [[& {:as keys}] key]
            (get keys key))]
    (step-for-keys state succ fail get-key)))

(defmethod step* `s/cat [state succ fail]
  (step-by-key state succ fail :val-fn nth))

(defmethod step* `s/& [state succ fail]
  (let [args (get-in state [:spec :args])
        specs (cons (:regex args) (:preds args))]
    (choose-spec specs state succ fail)))

(defmethod step* `s/alt [state succ fail]
  (step-by-key state succ fail))

(defmethod step* `s/* [{:keys [spec] :as state} succ fail]
  (-> state
      (update :spec get-in [:args :pred-form])
      (assoc :skip? true)
      (succ fail)))

(defmethod step* `s/+ [{:keys [spec] :as state} succ fail]
  (-> state
      (update :spec get-in [:args :pred-form])
      (assoc :skip? true)
      (succ fail)))

(defn- step [{:keys [spec] :as state} succ fail]
  (if (or (set? spec) (symbol? spec) (keyword? spec))
    (succ state fail)
    (step* state succ fail)))

(defn- parse-spec [spec]
  (let [result (s/conform ::specs/spec (s/form spec))]
    (if (= result ::s/invalid)
      result
      (let [[kind spec] result]
        spec))))

(defn- conformed-spec [spec]
  (let [spec' (if (keyword? spec)
                (parse-spec spec)
                spec)]
    (assert (not= spec' ::s/invalid)
            (str "spec macro " spec " must have its own spec definition"))
    spec'))

(defn- normalize [{:keys [spec path val in]}]
  (let [spec' (conformed-spec spec)
        state {:spec spec'
               :path (vec path)
               :val val
               :in (vec in)}]
    (if (not= spec spec')
      (assoc state :spec-name spec)
      (dissoc state :spec-name))))

(defn trace [{:keys [path in val pred] :as problem} spec value]
  (letfn [(rec [{:keys [spec] :as state} fail ret]
            (if (or (= spec pred) (symbol? spec) (seq? spec))
              ret
              (step (assoc state :pred pred)
                    (fn [{:keys [spec skip?] :as state'} fail]
                      (let [spec (if (keyword? spec) spec (second spec))
                            state' (normalize (assoc state' :spec spec))
                            ret' (if skip? ret (conj ret state'))]
                        #(rec state' fail ret')))
                    (fn [] fail))))]
    (let [state (normalize {:spec spec :path path :val value :in in})]
      (trampoline rec state (constantly nil) [state]))))

(defn traces
  ([ed] (traces ed nil))
  ([ed value]
   (mapv #(trace % (first (:via %)) value)
         (::s/problems ed))))

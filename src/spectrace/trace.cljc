(ns spectrace.trace
  (:require [clojure.spec :as s]
            [spectrace.specs :as specs]))

(s/def ::s symbol?)
(s/def ::spec (s/keys :req-un [::s]))
(s/def ::path (s/coll-of keyword?))
(s/def ::val any?)
(s/def ::in (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::spec-name keyword?)

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in]
          :opt-un [::spec-name]))

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

(defn- resolve-spec [spec]
  (cond-> spec
    (keyword? spec) s/form))

(defn step [{:keys [spec] :as state} succ fail]
  (let [spec (resolve-spec spec)]
    (if (symbol? spec)
      (succ (assoc state :spec spec) fail)
      (let [spec' (s/conform ::specs/spec spec)]
        (assert (not= spec' ::s/invalid)
                (str "spec macro " spec
                     " must have its own spec definition"))
        (step* (assoc state :spec spec') succ fail)))))

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
      (when-let [spec' (some #(and (= (:key %) segment) (:pred %))
                             (:args spec))]
        {:spec spec' :path path :in in
         :val (cond-> val val-fn #(val-fn % key))}))))

(defmethod step* `s/or [state succ fail]
  (step-by-key state succ fail))

(defmethod step* `s/cat [state succ fail]
  (step-by-key state succ fail :val-fn nth))

(defmethod step* `s/& [state succ fail]
  (let [args (get-in state [:spec :args])
        specs (cons (:re args) (:preds args))]
    (choose-spec specs state succ fail)))

(defmethod step* `s/alt [state succ fail]
  (step-by-key state succ fail))

(defmethod step* `s/* [{:keys [spec] :as state} succ fail]
  (step (assoc state :spec (get-in spec [:args :pred-form])) succ fail))

(defmethod step* `s/+ [{:keys [spec] :as state} succ fail]
  (step (assoc state :spec (get-in spec [:args :pred-form])) succ fail))

(defn- normalize [{:keys [spec path val in]}]
  (let [spec' (resolve-spec spec)
        state {:spec spec'
               :path (vec path)
               :val val
               :in (vec in)}]
    (if (not= spec spec')
      (assoc state :spec-name spec)
      (dissoc state :spec-name))))

(defn trace [{:keys [path in val] :as problem} spec value]
  (letfn [(rec [{:keys [path] :as state} fail ret]
            (if (empty? path)
              ret
              (step state
                    (fn [state' fail]
                      (let [state' (normalize state')]
                        #(rec state' fail (conj ret state'))))
                    (fn [] fail))))]
    (let [state (normalize {:spec spec :path path :val value :in in})]
      (trampoline rec state (constantly nil) [state]))))

(defn traces
  ([ed] (traces ed nil))
  ([ed value]
   (mapv #(trace % (first (:via %)) value)
         (::s/problems ed))))

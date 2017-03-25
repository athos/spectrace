(ns spectrace.trace
  (:require [clojure.spec :as s]
            [spectrace.specs :as specs]))

(s/def ::s symbol?)
(s/def ::spec (s/keys :req-un [::s]))
(s/def ::path (s/coll-of keyword?))
(s/def ::val any?)
(s/def ::in (s/coll-of (s/or :keyword keyword? :int integer?)))

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in]))

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

(defn- with-cont [succ fail f]
  (if-let [ret (f)]
    (succ ret fail)
    (fail)))

(defmethod step* `s/cat [state succ fail]
  (with-cont succ fail
    (fn []
      (let [{[segment & path] :path :keys [spec val in]} state]
        (when-let [spec' (get (:args spec) segment)]
          {:spec spec' :path path :in (rest in)
           :val (nth val (first in))})))))

(defmethod step* `s/& [state succ fail]
  (letfn [(rec [specs]
            (if (empty? specs)
              (fail)
              (succ (assoc state :spec (first specs))
                    #(rec (rest specs)))))]
    (let [args (get-in state [:spec :args])
          specs (cons (:re args) (:preds args))]
      (rec specs))))

(defmethod step* `s/alt [state succ fail]
  (with-cont succ fail
    (fn []
      (let [{[segment & path] :path :keys [spec val in]} state]
        (when-let [spec' (some #(and (= (:key %) segment) (:pred %))
                               (:args spec))]
          {:spec spec' :path path :in (rest in)
           :val (nth val (first in))})))))

(defmethod step* `s/* [{:keys [spec] :as state} succ fail]
  (step (assoc state :spec (get-in spec [:args :pred-form])) succ fail))

(defmethod step* `s/+ [{:keys [spec] :as state} succ fail]
  (step (assoc state :spec (get-in spec [:args :pred-form])) succ fail))

(defn- step [{:keys [spec] :as state} succ fail]
  (let [spec (cond-> spec
               (keyword? spec) s/form)
        spec' (s/conform ::specs/spec spec)]
    (assert (not= spec' ::s/invalid)
            (str "spec macro " (first spec)
                 " must have its own spec definition"))
    (step* (assoc state :spec spec) succ fail)))

(defn- trace* [{:keys [path] :as state} fail ret]
  (if (empty? path)
    ret
    (step state
          (fn [state' fail]
            (let [state' (-> state'
                             (update :path vec)
                             (update :in vec))]
              #(trace* state' fail (conj ret state'))))
          (fn [] fail))))

(defn trace [{:keys [path in pred val] :as problem} spec value]
  (let [state {:spec spec :path path :val value :in in}]
    (-> (trampoline trace* state (constantly nil) [state])
        vec
        (conj {:spec pred :path [] :val val :in []}))))

(defn traces
  ([ed] (traces ed nil))
  ([ed value]
   (mapv #(trace % (first (:via %)) value)
         (::s/problems ed))))

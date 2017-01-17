(ns spectrace.trace
  (:require [clojure.spec :as s]))

(defmulti step (fn [op conformed path] op))
(defmethod step :default [_ _ _] nil)

(defmethod step `s/keys [_ conformed [segment & path]]
  [segment path])

(defmethod step `s/cat [_ conformed [segment & path]]
  [(:pred (some #(and (= (:key %) segment) %) conformed))
   path])

(defmethod step `s/alt [_ conformed [segment & path]]
  [(:pred (some #(and (= (:key %) segment) %) conformed))
   path])

(defmethod step `s/* [_ conformed path]
  [(:pred-form conformed) path])

(defn conform-macro-form [[op & args]]
  (let [var (resolve op)]
    (s/conform (:args (s/get-spec var)) args)))

(defn trace [{:keys [path] :as problem}]
  (let [form (s/form (s/get-spec (first (:via problem))))]
    (loop [path path, form form, ret []]
      ;(prn :path path)
      ;(prn :form form)
      (if (empty? path)
        ret
        (let [[form' path'] (step (first form)
                                  (conform-macro-form form)
                                  path)]
          (recur path'
                 (if (keyword? form') (s/form (s/get-spec form')) form')
                 (conj ret {:form form})))))))

(defn traces [ed]
  (mapv trace (::s/problems ed)))

(ns spectrace.trace
  (:require [clojure.spec :as s]
            [spectrace.specs :as specs]))

(defmulti step (fn [conformed path] (:s conformed)))
(defmethod step :default [_ _] nil)

(defmethod step `s/keys [_ [segment & path]]
  [segment path])

(defmethod step `s/cat [{:keys [args]} [segment & path]]
  [(:pred (some #(and (= (:key %) segment) %) args))
   path])

(defmethod step `s/alt [{:keys [args]} [segment & path]]
  [(:pred (some #(and (= (:key %) segment) %) args))
   path])

(defmethod step `s/* [{:keys [args]} path]
  [(:pred-form args) path])

(defn trace [{:keys [path] :as problem}]
  (let [form (s/form (s/get-spec (first (:via problem))))]
    (loop [path path, form form, ret []]
      ;(prn :path path)
      ;(prn :form form)
      (if (empty? path)
        ret
        (let [[form' path'] (step (s/conform ::specs/spec form) path)]
          (recur path'
                 (if (keyword? form') (s/form (s/get-spec form')) form')
                 (conj ret {:form form})))))))

(defn traces [ed]
  (mapv trace (::s/problems ed)))

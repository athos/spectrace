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
  (let [spec (s/get-spec (first (:via problem)))]
    (loop [path path, spec spec, form (s/form spec), ret []]
      ;(prn :path path)
      ;(prn :spec spec)
      ;(prn :form form)
      (if (empty? path)
        ret
        (let [[spec' path'] (step (first form)
                                  (conform-macro-form form)
                                  path)
              [spec' form'] (if (keyword? spec')
                              (let [spec (s/get-spec spec')]
                                [spec (s/form spec)])
                              [spec spec'])]
          (recur path' spec' form'
                 (conj ret {:spec spec :form form})))))))

(defn traces [ed]
  (mapv trace (::s/problems ed)))

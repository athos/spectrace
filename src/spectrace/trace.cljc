(ns spectrace.trace
  (:require [clojure.spec :as s]))

(defmulti step (fn [var conformed path] var))
(defmethod step :default [_ _ _] nil)

(defmethod step #'s/keys [_ conformed [segment & path]]
  [segment path])

(defmethod step #'s/cat [_ conformed [segment & path]]
  [(:pred (some #(and (= (:key %) segment) %) conformed))
   path])

(defmethod step #'s/alt [_ conformed [segment & path]]
  [(:pred (some #(and (= (:key %) segment) %) conformed))
   path])

(defmethod step #'s/* [_ conformed path]
  [(:pred-form conformed) path])

(defn conform-macro-form [var form]
  (s/conform (:args (s/get-spec var)) (rest form)))

(defn trace [{:keys [path] :as problem}]
  (let [spec (s/get-spec (first (:via problem)))]
    (loop [path path, spec spec, form (s/form spec), ret []]
      ;(prn :path path)
      ;(prn :spec spec)
      ;(prn :form form)
      (if (empty? path)
        ret
        (let [v (resolve (first form))
              [spec' path'] (step v (conform-macro-form v form) path)]
          (let [[spec' form'] (if (keyword? spec')
                                (let [spec (s/get-spec spec')]
                                  [spec (s/form spec)])
                                [spec spec'])]
            (recur path' spec' form'
                   (conj ret {:spec spec :form form}))))))))

(defn traces [ed]
  (mapv trace (::s/problems ed)))

(ns spectrace.trace-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is are]]
            [spectrace.trace :as trace]))

(s/def ::x integer?)
(s/def ::y string?)

(deftest traces-test
  (are [spec data expected]
      (= expected (trace/traces (s/explain-data spec data)))

    (s/spec integer?)
    :a
    [[{:spec `integer? :path [] :val :a :in []}]]

    ::x
    :a
    [[{:spec `integer? :path [] :val :a :in []
       :spec-name ::x}]]

    (s/or :int integer? :str string?)
    :a
    [[{:spec `(s/or :int integer? :str string?)
       :path [:int]
       :val :a
       :in []}
      {:spec `integer? :path [] :val :a :in []}]
     [{:spec `(s/or :int integer? :str string?)
       :path [:str]
       :val :a
       :in []}
      {:spec `string? :path [] :val :a :in []}]]))

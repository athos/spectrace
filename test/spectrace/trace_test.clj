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

    (s/and integer? even?)
    3
    [[{:spec `(s/and integer? even?) :path [] :val 3 :in []}
      {:spec `even? :path [] :val 3 :in []}]]

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
      {:spec `string? :path [] :val :a :in []}]]

    (s/tuple integer? string?)
    [1 :a]
    [[{:spec `(s/tuple integer? string?) :path [1] :val [1 :a] :in [1]}
      {:spec `string? :path [] :val :a :in []}]]

    (s/map-of integer? string?)
    {:a :b}
    [[{:spec `(s/map-of integer? string?)
       :path [0]
       :val {:a :b}
       :in [:a 0]}
      {:spec `integer? :path [] :val :a :in []}]
     [{:spec `(s/map-of integer? string?)
       :path [1]
       :val {:a :b}
       :in [:a 1]}
      {:spec `string? :path [] :val :b :in []}]]

    (s/every-kv integer? string?)
    {:a :b}
    [[{:spec `(s/every-kv integer? string?)
       :path [0]
       :val {:a :b}
       :in [:a 0]}
      {:spec `integer? :path [] :val :a :in []}]
     [{:spec `(s/every-kv integer? string?)
       :path [1]
       :val {:a :b}
       :in [:a 1]}
      {:spec `string? :path [] :val :b :in []}]]

    (s/cat :int integer? :str string?)
    [1 :b]
    [[{:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 :b]
       :in [1]}
      {:spec `string? :path [] :val :b :in []}]]

    (s/& (s/cat :x integer? :y integer?)
         (fn [{:keys [x y]}] (< x y)))
    [4 :a]
    [[{:spec `(s/& (s/cat :x integer? :y integer?)
                   (fn [{:keys [~'x ~'y]}] (< ~'x ~'y)))
       :path [:y]
       :val [4 :a]
       :in [1]}
      {:spec `(s/cat :x integer? :y integer?)
       :path [:y]
       :val [4 :a]
       :in [1]}
      {:spec `integer? :path [] :val :a :in []}]]

    (s/& (s/cat :x integer? :y integer?)
         (fn [{:keys [x y]}] (< x y)))
    [4 3]
    [[{:spec `(s/& (s/cat :x integer? :y integer?)
                   (fn [{:keys [~'x ~'y]}] (< ~'x ~'y)))
       :path []
       :val [4 3]
       :in [1]}
      {:spec `(fn [{:keys [~'x ~'y]}] (< ~'x ~'y))
       :path []
       :val [4 3]
       :in [1]}]]

    (s/alt :int integer? :str string?)
    [:a]
    [[{:spec `(s/alt :int integer? :str string?)
       :path [:int]
       :val [:a]
       :in [0]}
      {:spec `integer? :path [] :val :a :in []}]
     [{:spec `(s/alt :int integer? :str string?)
       :path [:str]
       :val [:a]
       :in [0]}
      {:spec `string? :path [] :val :a :in []}]]

    (s/* integer?)
    [1 :a 3]
    [[{:spec `(s/* integer?) :path [] :val [1 :a 3] :in [1]}
      {:spec `integer? :path [] :val :a :in []}]]

    (s/+ integer?)
    [:a]
    [[{:spec `(s/+ integer?) :path [] :val [:a] :in [0]}
      {:spec `integer? :path [] :val :a :in []}]]

    ))

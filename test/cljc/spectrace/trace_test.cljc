(ns spectrace.trace-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is are]]
            #?@(:cljs ([clojure.test.check]
                       [clojure.test.check.generators]
                       [clojure.test.check.properties]))
            [spectrace.trace :as trace]))

(s/def ::x integer?)
(s/def ::y string?)
(s/def ::z keyword?)

(s/def ::type keyword?)
(defmulti m :type)
(defmethod m :x [_] (s/keys :req-un [::type ::x]))
(defmethod m :y [_] (s/keys :req-un [::type ::y]))

(deftest traces-test
  (are [spec data expected]
      (= expected (trace/traces (s/explain-data spec data)))

    (s/spec integer?)
    :a
    [[{:spec `integer? :path [] :val :a :in [] :trails []}]]

    ::x
    :a
    [[{:spec `integer? :path [] :val :a :in [] :trails []
       :spec-name ::x}]]

    (s/and integer? even?)
    3
    [[{:spec `(s/and integer? even?) :path [] :val 3 :in [] :trails []}
      {:spec `even? :path [] :val 3 :in [] :trails [1]}]]

    (s/or :int integer? :str string?)
    :a
    [[{:spec `(s/or :int integer? :str string?)
       :path [:int]
       :val :a
       :in []
       :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails [:int]}]
     [{:spec `(s/or :int integer? :str string?)
       :path [:str]
       :val :a
       :in []
       :trails []}
      {:spec `string? :path [] :val :a :in [] :trails [:str]}]]

    (s/nilable integer?)
    :a
    [[{:spec `(s/nilable integer?) :path [::s/pred] :val :a :in []
       :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]
     [{:spec `(s/nilable integer?) :path [::s/nil] :val :a :in []
       :trails []}
      {:spec 'nil? :path [] :val :a :in [] :trails []}]]

    (s/tuple integer? string?)
    [1 :a]
    [[{:spec `(s/tuple integer? string?) :path [1] :val [1 :a] :in [1]
       :trails []}
      {:spec `string? :path [] :val :a :in [] :trails [1]}]]

    (s/tuple integer?)
    {}
    [[{:spec `(s/tuple integer?) :path [] :val {} :in [] :trails []}
      {:spec 'vector? :path [] :val {} :in [] :trails []}]]

    (s/tuple integer? string?)
    [1 "foo" 3]
    [[{:spec `(s/tuple integer? string?)
       :path []
       :val [1 "foo" 3]
       :in []
       :trails []}
      {:spec `(= (count ~'%) 2) :path [] :val [1 "foo" 3] :in []
       :trails []}]]

    ;; we can omit `s/spec` once CLJ-2168 is fixed
    (s/coll-of (s/spec integer?))
    [1 :a]
    [[{:spec `(s/coll-of (s/spec integer?)) :path [] :val [1 :a] :in [1]
       :trails []}
      {:spec `(s/spec integer?) :path [] :val :a :in [] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    (s/coll-of (s/spec (fn [[k v]] (= (str k) v))))
    {1 :a}
    [[{:spec `(s/coll-of (s/spec (fn [[~'k ~'v]] (= (str ~'k) ~'v))))
       :path []
       :val {1 :a}
       :in [0]
       :trails []}
      {:spec `(s/spec (fn [[~'k ~'v]] (= (str ~'k) ~'v)))
       :path []
       :val [1 :a]
       :in []
       :trails []}
      {:spec `(fn [[~'k ~'v]] (= (str ~'k) ~'v))
       :path []
       :val [1 :a]
       :in []
       :trails []}]]

    (s/coll-of (s/spec integer?))
    #{1 :a}
    [[{:spec `(s/coll-of (s/spec integer?)) :path [] :val #{1 :a} :in [1]
       :trails []}
      {:spec `(s/spec integer?) :path [] :val :a :in [] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    ;; ditto
    (s/every (s/spec integer?))
    [1 :a]
    [[{:spec `(s/every (s/spec integer?)) :path [] :val [1 :a] :in [1]
       :trails []}
      {:spec `(s/spec integer?) :path [] :val :a :in [] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    (s/every (s/spec (fn [[k v]] (= (str k) v))))
    {1 :a}
    [[{:spec `(s/every (s/spec (fn [[~'k ~'v]] (= (str ~'k) ~'v))))
       :path []
       :val {1 :a}
       :in [0]
       :trails []}
      {:spec `(s/spec (fn [[~'k ~'v]] (= (str ~'k) ~'v)))
       :path []
       :val [1 :a]
       :in []
       :trails []}
      {:spec `(fn [[~'k ~'v]] (= (str ~'k) ~'v))
       :path []
       :val [1 :a]
       :in []
       :trails []}]]

    (s/every (s/spec integer?))
    #{1 :a}
    [[{:spec `(s/every (s/spec integer?)) :path [] :val #{1 :a} :in [1]
       :trails []}
      {:spec `(s/spec integer?) :path [] :val :a :in [] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    (s/map-of integer? string?)
    {:a :b}
    [[{:spec `(s/map-of integer? string?)
       :path [0]
       :val {:a :b}
       :in [:a 0]
       :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails [0]}]
     [{:spec `(s/map-of integer? string?)
       :path [1]
       :val {:a :b}
       :in [:a 1]
       :trails []}
      {:spec `string? :path [] :val :b :in [] :trails [1]}]]

    (s/every-kv integer? string?)
    {:a :b}
    [[{:spec `(s/every-kv integer? string?)
       :path [0]
       :val {:a :b}
       :in [:a 0]
       :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails [0]}]
     [{:spec `(s/every-kv integer? string?)
       :path [1]
       :val {:a :b}
       :in [:a 1]
       :trails []}
      {:spec `string? :path [] :val :b :in [] :trails [1]}]]

    (s/keys :req-un [::x ::y])
    {}
    [[{:spec `(s/keys :req-un [::x ::y]) :path [] :val {} :in []
       :trails []}
      {:spec `(fn [~'%] (contains? ~'% :x)) :path [] :val {} :in []
       :trails []}]
     [{:spec `(s/keys :req-un [::x ::y]) :path [] :val {} :in []
       :trails []}
      {:spec `(fn [~'%] (contains? ~'% :y)) :path [] :val {} :in []
       :trails []}]]

    (s/keys :opt-un [::x])
    {:x :a}
    [[{:spec `(s/keys :opt-un [::x]) :path [:x] :val {:x :a} :in [:x]
       :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails [:x] :spec-name ::x}]]

    (s/keys :req [(or ::x (and ::y ::z))])
    {::y "foo" ::z 42}
    [[{:spec `(s/keys :req [(~'or ::x (~'and ::y ::z))])
       :path [::z]
       :val {::y "foo" ::z 42}
       :in [::z]
       :trails []}
      {:spec `keyword? :path [] :val 42 :in [] :trails [::z] :spec-name ::z}]]

    (s/merge (s/keys :req-un [::x]) (s/keys :req-un [::y]))
    {:x :a}
    [[{:spec `(s/merge (s/keys :req-un [::x]) (s/keys :req-un [::y]))
       :path [:x]
       :val {:x :a}
       :in [:x]
       :trails []}
      {:spec `(s/keys :req-un [::x]) :path [:x] :val {:x :a} :in [:x]
       :trails [0]}
      {:spec `integer? :path [] :val :a :in [] :spec-name ::x
       :trails [0 :x]}]
     [{:spec `(s/merge (s/keys :req-un [::x]) (s/keys :req-un [::y]))
       :path []
       :val {:x :a}
       :in []
       :trails []}
      {:spec `(s/keys :req-un [::y]) :path [] :val {:x :a} :in []
       :trails [1]}
      {:spec `(fn [~'%] (contains? ~'% :y)) :path [] :val {:x :a} :in []
       :trails [1]}]]

    (s/cat :int integer? :str string?)
    [1 :b]
    [[{:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 :b]
       :in [1]
       :trails []}
      {:spec `string? :path [] :val :b :in [] :trails [:str]}]]

    (s/cat :int integer? :more (s/cat :int integer? :str string?))
    [1 2 3]
    [[{:spec `(s/cat :int integer?
                     :more (s/cat :int integer? :str string?))
       :path [:more :str]
       :val [1 2 3]
       :in [2]
       :trails []}
      {:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 2 3]
       :in [2]
       :trails [:more]}
      {:spec `string? :path [] :val 3 :in [] :trails [:more :str]}]]

    ;; Add this after CLJ-2178 is fixed
    #_(s/& integer? even?)
    #_[1]
    #_[[{:spec `(s/& integer? even?) :path [] :val [1] :in [0] :trails []}
      {:spec `even? :path [] :val 1 :in [] :trails [1]}]]

    (s/& (s/cat :x integer? :y integer?)
         (fn [{:keys [x y]}] (< x y)))
    [4 :a]
    [[{:spec `(s/& (s/cat :x integer? :y integer?)
                   (fn [{:keys [~'x ~'y]}] (< ~'x ~'y)))
       :path [:y]
       :val [4 :a]
       :in [1]
       :trails []}
      {:spec `(s/cat :x integer? :y integer?)
       :path [:y]
       :val [4 :a]
       :in [1]
       :trails [0]}
      {:spec `integer? :path [] :val :a :in [] :trails [0 :y]}]]

    #_(s/& (s/cat :x integer? :y integer?)
         (fn [{:keys [x y]}] (< x y)))
    #_[4 3]
    #_[[{:spec `(s/& (s/cat :x integer? :y integer?)
                   (fn [{:keys [~'x ~'y]}] (< ~'x ~'y)))
       :path []
       :val [4 3]
       :in [1]
       :trails []}
      {:spec `(fn [{:keys [~'x ~'y]}] (< ~'x ~'y))
       :path []
       :val [4 3]
       :in [1]
       :trails [1]}]]

    (s/alt :int integer? :str string?)
    [:a]
    [[{:spec `(s/alt :int integer? :str string?)
       :path [:int]
       :val [:a]
       :in [0]
       :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails [:int]}]
     [{:spec `(s/alt :int integer? :str string?)
       :path [:str]
       :val [:a]
       :in [0]
       :trails []}
      {:spec `string? :path [] :val :a :in [] :trails [:str]}]]

    (s/alt :one integer? :two (s/cat :first integer? :second integer?))
    [1 'foo]
    [[{:spec `(s/alt :one integer?
                     :two (s/cat :first integer? :second integer?))
       :path [:two :second]
       :val [1 'foo]
       :in [1]
       :trails []}
      {:spec `(s/cat :first integer? :second integer?)
       :path [:second]
       :val [1 'foo]
       :in [1]
       :trails [:two]}
      {:spec `integer? :path [] :val 'foo :in [] :trails [:two :second]}]]

    (s/? integer?)
    [:a]
    [[{:spec `(s/? integer?) :path [] :val [:a] :in [0] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    (s/? (s/cat :int integer? :str string?))
    [1 :a]
    [[{:spec `(s/? (s/cat :int integer? :str string?))
       :path [:str]
       :val [1 :a]
       :in [1]
       :trails []}
      {:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 :a]
       :in [1]
       :trails []}
      {:spec `string? :path [] :val :a :in [] :trails [:str]}]]

    (s/* integer?)
    [1 :a 3]
    [[{:spec `(s/* integer?) :path [] :val [1 :a 3] :in [1] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    (s/* (s/cat :int integer? :str string?))
    [1 "foo" 2 :bar]
    [[{:spec `(s/* (s/cat :int integer? :str string?))
       :path [:str]
       :val [1 "foo" 2 :bar]
       :in [3]
       :trails []}
      {:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 "foo" 2 :bar]
       :in [3]
       :trails []}
      {:spec `string? :path [] :val :bar :in [] :trails [:str]}]]

    (s/+ integer?)
    [:a]
    [[{:spec `(s/+ integer?) :path [] :val [:a] :in [0] :trails []}
      {:spec `integer? :path [] :val :a :in [] :trails []}]]

    (s/+ (s/cat :int integer? :str string?))
    [1 "foo" 2 :bar]
    [[{:spec `(s/+ (s/cat :int integer? :str string?))
       :path [:str]
       :val [1 "foo" 2 :bar]
       :in [3]
       :trails []}
      {:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 "foo" 2 :bar]
       :in [3]
       :trails []}
      {:spec `string? :path [] :val :bar :in [] :trails [:str]}]]

    (s/fspec :args (s/cat :n integer?) :ret integer?)
    str
    [[{:spec `(s/fspec :args (s/cat :n integer?) :ret integer? :fn nil)
       :path [:ret]
       :val str
       :in []
       :trails []}
      {:spec `integer? :path [] :val str :in [] :trails [:ret]}]]

    (s/multi-spec m :type)
    {:type :x}
    [[{:spec `(s/multi-spec m :type) :path [:x] :val {:type :x} :in []
       :trails []}
      {:spec `(s/keys :req-un [::type ::x])
       :path []
       :val {:type :x}
       :in []
       :trails [:x]}
      {:spec `(fn [~'%] (contains? ~'% :x))
       :path []
       :val {:type :x}
       :in []
       :trails [:x]}]]

    ;; Add this though I'm not sure it's proper usage of s/conformer
    (s/conformer (constantly ::s/invalid))
    42
    [[{:spec `(s/conformer (constantly ::s/invalid))
       :path []
       :val 42
       :in []
       :trails []}]]

    (s/nonconforming (s/cat :int integer? :str string?))
    [1 'foo]
    [[{:spec `(s/nonconforming (s/cat :int integer? :str string?))
       :path [:str]
       :val [1 'foo]
       :in [1]
       :trails []}
      {:spec `(s/cat :int integer? :str string?)
       :path [:str]
       :val [1 'foo]
       :in [1]
       :trails []}
      {:spec `string? :path [] :val 'foo :in [] :trails [:str]}]]

    ))

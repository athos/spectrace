(ns spectrace.v0-2-168-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [spectrace.core :as strace]))

(deftest CLJ-2068-test
  (is (= [[{:spec `integer? :path [] :val :foo :in [] :trail []}]]
         (strace/traces (s/explain-data integer? :foo)))))

(deftest CLJ-2176-test
  (is (= [[{:spec `(s/tuple integer?) :path [] :val {} :in [] :trail []}
           {:spec `vector? :path [] :val {} :in [] :trail []}]]
         (strace/traces (s/explain-data (s/tuple integer?) {})))))

(deftest CLJ-2177-test
  (is (= [[{:spec `(s/keys :req-un [::x ::y]) :path [] :val 1 :in [] :trail []}
           {:spec `map? :path [] :val 1 :in [] :trail []}]]
         (strace/traces (s/explain-data (s/keys :req-un [::x ::y]) 1)))))

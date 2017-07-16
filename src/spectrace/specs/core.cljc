(ns spectrace.specs.core
  (:require [clojure.spec.alpha :as s]
            [spectrace.core :as strace]))

(s/def ::spec any?)
(s/def ::path (s/coll-of any?))
(s/def ::val any?)
(s/def ::in (s/coll-of any?))
(s/def ::pred any?)
(s/def ::spec-name keyword?)
(s/def ::trails (s/coll-of any?))

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in ::trails]
          :opt-un [::spec-name]))

(s/fdef strace/step*
  :args (s/cat :state ::state)
  :ret ::state)

(s/fdef strace/step
  :args (s/cat :state ::state)
  :ret ::state)

(s/def ::problem
  (s/keys :req-un [::pred ::path ::val ::in]))
(s/def ::s/spec
  (s/or :spec s/spec? :regex s/regex? :fn fn? :set set?))
(s/def ::s/value any?)

(s/fdef strace/trace
  :args (s/cat :problem ::s/problem
               :spec  ::s/spec
               :value ::s/value)
  :ret (s/coll-of ::state))

(s/def ::s/problems (s/coll-of ::problem))
(s/def ::explain-data
  (s/keys :req [::s/spec ::s/value ::s/problems]))

(s/fdef strace/traces
  :args (s/cat :ed ::explain-data)
  :ret (s/coll-of (s/coll-of ::state)))

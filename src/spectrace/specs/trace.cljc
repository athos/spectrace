(ns spectrace.specs.trace
  (:require [clojure.spec.alpha :as s]
            [spectrace.trace :as trace]))

(s/def ::spec any?)
(s/def ::path (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::val any?)
(s/def ::in (s/coll-of (s/or :keyword keyword? :int integer?)))
(s/def ::pred any?)
(s/def ::spec-name keyword?)

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in ::pred]
          :opt-un [::spec-name]))

(s/fdef trace/step*
  :args (s/cat :state ::state))

(s/fdef trace/step
  :args (s/cat :state ::state))

(s/def ::problem
  (s/keys :req-un [::pred ::path ::val ::in]))
(s/def ::s/spec
  (s/or :spec s/spec? :regex s/regex? :fn fn? :set set?))
(s/def ::s/value any?)

(s/fdef trace/trace
  :args (s/cat :problem ::s/problem
               :spec  ::s/spec
               :value ::s/value)
  :ret (s/coll-of ::state))

(s/def ::s/problems (s/coll-of ::problem))
(s/def ::explain-data
  (s/keys :req [::s/spec ::s/value ::s/problems]))

(s/fdef trace/traces
  :args (s/cat :ed ::explain-data)
  :ret (s/coll-of (s/coll-of ::state)))

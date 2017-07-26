(ns spectrace.specs.core
  (:require [clojure.spec.alpha :as s]
            [spectrace.core :as strace]))

(s/def ::spec any?)
(s/def ::path (s/nilable (s/coll-of any?)))
(s/def ::val any?)
(s/def ::in (s/nilable (s/coll-of any?)))
(s/def ::pred any?)
(s/def ::reason string?)
(s/def ::trail (s/coll-of any?))
(s/def ::spec-name keyword?)
(s/def ::snapshots (s/coll-of any?))

(s/def ::state
  (s/keys :req-un [::spec ::path ::val ::in ::trail]
          :opt-un [::reason ::spec-name ::snapshots]))

(s/fdef strace/step
  :args (s/cat :state ::state)
  :ret (s/nilable ::state))

(s/def ::problem
  (s/keys :req-un [::pred ::path ::val ::in]))
(s/def ::s/spec
  (s/or :spec s/spec? :regex s/regex? :fn fn? :set set? :keyword keyword?))
(s/def ::s/value any?)

(s/fdef strace/trace
  :args (s/cat :problem ::problem
               :spec  ::s/spec
               :value ::s/value)
  :ret (s/coll-of ::state))

(s/def ::s/problems (s/coll-of ::problem))
(s/def ::explain-data
  (s/keys :req [::s/spec ::s/value ::s/problems]))

(s/fdef strace/traces
  :args (s/cat :ed ::explain-data)
  :ret (s/coll-of (s/coll-of ::state)))

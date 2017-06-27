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

(s/def ::fail (s/fspec :args (s/cat) :ret any?))
(s/def ::succ (s/fspec :args (s/cat :state ::state :fail ::fail) :ret any?))

(s/fdef trace/step*
  :args (s/cat :state ::state
               :succ  ::succ
               :fail  ::fail))

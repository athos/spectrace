(ns spectrace.specs
  (:require [clojure.spec :as s]
            [clojure.core.specs :as specs]))

;; These spec definitions for spec macros are intended to be only used
;; internally just for now, and will be removed when the official
;; definitions are included in a future release of Clojure 1.9.

(s/def ::specs/gen any?)

(s/fdef s/spec
  :args (s/cat :form any? :opts (s/keys* :opt-un [::specs/gen])))

(s/fdef s/and
  :args (s/* any?))

(s/def ::key+pred-pairs
  (s/* (s/cat :key keyword? :pred any?)))

(s/fdef s/or
  :args ::key+pred-pairs)

(s/fdef s/nilable
  :args (s/cat :pred any?))

(s/fdef s/tuple
  :args (s/+ any?))

(s/def ::specs/into #{[] {} #{}})
(s/def ::specs/kind any?)
(s/def ::specs/count any?)
(s/def ::specs/max-count any?)
(s/def ::specs/min-count any?)
(s/def ::specs/distinct any?)
(s/def ::specs/gen-max any?)

(s/def ::every-opts
  (s/keys* :opt-un [::specs/into ::specs/kind ::specs/count
                    ::specs/max-count ::specs/min-count
                    ::specs/distinct ::specs/gen-max ::specs/gen]))

(s/fdef s/coll-of
  :args (s/cat :pred any? :opts ::every-opts))

(s/fdef s/map-of
  :args (s/cat :kpred any? :vpred any? :opts ::every-opts))

(s/fdef s/every
  :args (s/cat :pred any? :opts ::every-opts))

(s/fdef s/every-kv
  :args (s/cat :kpred any? :vpred any? :opts ::every-opts))

(s/def ::spec-name qualified-keyword?)
(s/def ::spec-group
  (s/or :spec ::spec-name
        :and  (s/cat :op '#{and} :specs (s/* ::spec-group))
        :or   (s/cat :op '#{or}  :specs (s/* ::spec-group))))
(s/def ::specs/req
  (s/* (s/alt :spec ::spec-name
              :spec-group ::spec-group)))
(s/def ::specs/req-un ::specs/req)
(s/def ::specs/opt (s/* ::spec-name))
(s/def ::specs/opt-un ::specs/opt)
(s/def ::keys-args
  (s/keys* :opt-un [::specs/req ::specs/req-un
                    ::specs/opt ::specs/opt-un]))

(s/fdef s/keys
  :args ::keys-args)

(s/fdef s/keys*
  :args ::keys-args)

(s/fdef s/merge
  :args (s/* any?))

(s/fdef s/cat
  :args ::key+pred-pairs)

(s/fdef s/&
  :args (s/cat :re any? :preds (s/* any?)))

(s/fdef s/alt
  :args ::key+pred-pairs)

(s/fdef s/?
  :args (s/cat :pred-form any?))

(s/fdef s/*
  :args (s/cat :pred-form any?))

(s/fdef s/+
  :args (s/cat :pred-form any?))

(s/def ::specs/args any?)
(s/def ::specs/ret any?)
(s/def ::specs/fn any?)

(s/fdef s/fspec
  :args (s/keys* :opt-un [::specs/args ::specs/ret
                          ::specs/fn ::specs/gen]))

(s/fdef s/multi-spec
  :args (s/cat :mm any? :retag any?))

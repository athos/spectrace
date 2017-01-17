(ns spectrace.specs
  (:require [clojure.spec :as s]
            [clojure.core.specs :as specs]))

;; These spec definitions for spec macros are intended to be only used
;; internally just for now, and will be removed when the official
;; definitions are included in a future release of Clojure 1.9.

(defmulti specs-spec
  (fn [form]
    (when (seq? form) (first form))))
(defmethod specs-spec :default [_] any?)

(s/def ::spec (s/multi-spec specs-spec :s))

(s/def ::gen any?)

(defmethod specs-spec `s/spec [_]
  (s/cat :s #{`s/spec}
         :args (s/cat :form any? :opts (s/keys* :opt-un [::gen]))))

(defmethod specs-spec `s/and [_]
  (s/cat :s #{`s/and}
         :args (s/* any?)))

(s/def ::key+pred-pairs
  (s/* (s/cat :key keyword? :pred any?)))

(defmethod specs-spec `s/or [_]
  (s/cat :s #{`s/or}
         :args ::key+pred-pairs))

(defmethod specs-spec `s/nilable [_]
  (s/cat :s #{`s/nilable}
         :args (s/cat :pred any?)))

(defmethod specs-spec `s/tuple [_]
  (s/cat :s #{`s/tuple}
         :args (s/+ any?)))

(s/def ::into #{[] {} #{}})
(s/def ::kind any?)
(s/def ::count any?)
(s/def ::max-count any?)
(s/def ::min-count any?)
(s/def ::distinct any?)
(s/def ::gen-max any?)

(s/def ::every-opts
  (s/keys* :opt-un [::into ::kind ::count
                    ::max-count ::min-count
                    ::distinct ::gen-max ::gen]))

(defmethod specs-spec `s/coll-of [_]
  (s/cat :s #{`s/coll-of}
         :args (s/cat :pred any? :opts ::every-opts)))

(defmethod specs-spec `s/map-of [_]
  (s/cat :s #{`s/map-of}
         :args (s/cat :kpred any? :vpred any? :opts ::every-opts)))

(defmethod specs-spec `s/every [_]
  (s/cat :s #{`s/every}
         :args (s/cat :pred any? :opts ::every-opts)))

(defmethod specs-spec `s/every-kv [_]
  (s/cat :s #{`s/every-kv}
         :args (s/cat :kpred any? :vpred any? :opts ::every-opts)))

(s/def ::spec-name qualified-keyword?)
(s/def ::spec-group
  (s/or :spec ::spec-name
        :and  (s/cat :op '#{and} :specs (s/* ::spec-group))
        :or   (s/cat :op '#{or}  :specs (s/* ::spec-group))))
(s/def ::req
  (s/* (s/alt :spec ::spec-name
              :spec-group ::spec-group)))
(s/def ::req-un ::req)
(s/def ::opt (s/* ::spec-name))
(s/def ::opt-un ::opt)
(s/def ::keys-args
  (s/keys* :opt-un [::req ::req-un ::opt ::opt-un]))

(defmethod specs-spec `s/keys [_]
  (s/cat :s #{`s/keys}
         :args ::keys-args))

(defmethod specs-spec `s/keys* [_]
  (s/cat :s #{`s/keys*}
         :args ::keys-args))

(defmethod specs-spec `s/merge [_]
  (s/cat :s #{`s/merge}
         :args (s/* any?)))

(defmethod specs-spec `s/cat [_]
  (s/cat :s #{`s/cat}
         :args ::key+pred-pairs))

(defmethod specs-spec `s/& [_]
  (s/cat :s #{`s/&}
         :args (s/cat :re any? :preds (s/* any?))))

(defmethod specs-spec `s/alt [_]
  (s/cat :s #{`s/alt}
         :args ::key+pred-pairs))

(defmethod specs-spec `s/? [_]
  (s/cat :s #{`s/?}
         :args (s/cat :pred-form any?)))

(defmethod specs-spec `s/* [_]
  (s/cat :s #{`s/*}
         :args (s/cat :pred-form any?)))

(defmethod specs-spec `s/+ [_]
  (s/cat :s #{`s/+}
         :args (s/cat :pred-form any?)))

(s/def ::args any?)
(s/def ::ret any?)
(s/def ::fn any?)

(defmethod specs-spec `s/fspec [_]
  (s/cat :s #{`s/fspec}
         :args (s/keys* :opt-un [::args ::ret ::fn ::gen])))

(defmethod specs-spec `s/multi-spec [_]
  (s/cat :s #{`s/multi-spec}
         :args (s/cat :mm any? :retag any?)))

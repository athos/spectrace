(ns spectrace.specs
  (:require [clojure.core :as cc]
            [clojure.spec.alpha :as s]))

;; These spec definitions for spec macros are intended to be only used
;; internally just for now, and will be removed when the official
;; definitions are included in a future release of Clojure 1.9.

(defmulti spec-form first)

(s/def ::spec
  (s/or :set set?
        :pred symbol?
        :keyword qualified-keyword?
        :form (s/multi-spec spec-form (fn [val tag] val))))

(defmethod spec-form `cc/fn [_]
  (s/cat :f #{`cc/fn}
         :args (s/and vector? #(= 1 (count %)))
         :body (s/* any?)))

(s/def ::gen ifn?)

(defmethod spec-form `s/spec [_]
  (s/cat :s #{`s/spec}
         :args (s/cat :form any? :opts (s/keys* :opt-un [::gen]))))

(defmethod spec-form `s/and [_]
  (s/cat :s #{`s/and}
         :args (s/* ::spec)))

(s/def ::tag+spec-pairs
  (s/* (s/cat :tag keyword? :spec ::spec)))

(defmethod spec-form `s/or [_]
  (s/cat :s #{`s/or}
         :args ::tag+spec-pairs))

(defmethod spec-form `s/nilable [_]
  (s/cat :s #{`s/nilable}
         :args (s/cat :spec ::spec)))

(defmethod spec-form `s/tuple [_]
  (s/cat :s #{`s/tuple}
         :args (s/* ::spec)))

(s/def ::into (s/and coll? empty?))
(s/def ::kind ifn?)
(s/def ::count nat-int?)
(s/def ::max-count nat-int?)
(s/def ::min-count nat-int?)
(s/def ::distinct boolean?)
(s/def ::gen-max nat-int?)

(s/def ::coll-opts
  (s/keys* :opt-un [::into ::kind ::count
                    ::max-count ::min-count
                    ::distinct ::gen-max ::gen]))

(defmethod spec-form `s/coll-of [_]
  (s/cat :s #{`s/coll-of}
         :args (s/cat :spec ::spec :opts ::coll-opts)))

(defmethod spec-form `s/map-of [_]
  (s/cat :s #{`s/map-of}
         :args (s/cat :kpred ::spec :vpred ::spec :opts ::coll-opts)))

(defmethod spec-form `s/every [_]
  (s/cat :s #{`s/every}
         :args (s/cat :spec ::spec :opts ::coll-opts)))

(defmethod spec-form `s/every-kv [_]
  (s/cat :s #{`s/every-kv}
         :args (s/cat :kpred ::spec :vpred ::spec :opts ::coll-opts)))

(s/def ::key
  (s/or :key qualified-keyword?
        :and (s/cat :and '#{and} :keys (s/* ::key))
        :or  (s/cat :or '#{or}  :keys (s/* ::key))))
(s/def ::req (s/coll-of ::key :kind vector?))
(s/def ::req-un ::req)
(s/def ::opt (s/coll-of qualified-keyword? :kind vector?))
(s/def ::opt-un ::opt)
(s/def ::keys-args
  (s/keys* :opt-un [::req ::req-un ::opt ::opt-un ::gen]))

(defmethod spec-form `s/keys [_]
  (s/cat :s #{`s/keys}
         :args ::keys-args))

(defmethod spec-form `s/keys* [_]
  (s/cat :s #{`s/keys*}
         :args ::keys-args))

(defmethod spec-form `s/merge [_]
  (s/cat :s #{`s/merge}
         :args (s/* ::spec)))

(defmethod spec-form `s/cat [_]
  (s/cat :s #{`s/cat}
         :args ::tag+spec-pairs))

(defmethod spec-form `s/& [_]
  (s/cat :s #{`s/&}
         :args (s/cat :regex ::spec :preds (s/* ::spec))))

(defmethod spec-form `s/alt [_]
  (s/cat :s #{`s/alt}
         :args ::tag+spec-pairs))

(defmethod spec-form `s/? [_]
  (s/cat :s #{`s/?}
         :args (s/cat :pred-form ::spec)))

(defmethod spec-form `s/* [_]
  (s/cat :s #{`s/*}
         :args (s/cat :pred-form ::spec)))

(defmethod spec-form `s/+ [_]
  (s/cat :s #{`s/+}
         :args (s/cat :pred-form ::spec)))

(s/def ::args ::spec)
(s/def ::ret ::spec)
(s/def ::fn ::spec)

(defmethod spec-form `s/fspec [_]
  (s/cat :s #{`s/fspec}
         :args (s/keys* :opt-un [::args ::ret ::fn])))

(defmethod spec-form `s/multi-spec [_]
  (s/cat :s #{`s/multi-spec}
         :args (s/cat :mm qualified-keyword?
                      :retag (s/alt :k keyword? :f ifn?))))

(defmethod spec-form `s/conformer [_]
  (s/cat :fn ifn? :unfn (s/? ifn?)))

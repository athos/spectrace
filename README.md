# spectrace [![Clojars Project](https://img.shields.io/clojars/v/spectrace.svg)](https://clojars.org/spectrace) [![CircleCI](https://circleci.com/gh/athos/spectrace.svg?style=svg)](https://circleci.com/gh/athos/spectrace)

clojure.spec (spec.alpha) library aiming to be a fundamental tool for analyzing spec errors

## Install

Add the following to your `:dependencies`:

[![Clojars Project](https://clojars.org/spectrace/latest-version.svg)](http://clojars.org/spectrace)

## Usage

In clojure.spec (spec.alpha), `s/explain-data` reports how spec conformance eventually failed, as follows:

```clj
user=> (require '[clojure.spec.alpha :as s])
nil
user=> (s/def ::x integer?)
:user/x
user=> (s/def ::y string?)
:user/y
user=> (s/def ::m (s/merge (s/keys :req-un [::x]) (s/keys :req-un [::y])))
:user/m
user=> (def ed (s/explain-data ::m {:x :a}))
#'user/ed
user=> ed
#:clojure.spec.alpha{:problems
                     ({:path [:x], :pred clojure.core/integer?,
                       :val :a, :via [:user/m :user/x], :in [:x]}
                      {:path [], :pred (clojure.core/fn [%] (clojure.core/contains? % :y)),
                       :val {:x :a}, :via [:user/m], :in []}), :spec :user/m, :value {:x :a}}
```

This might be useful enough as it is to make simple error messages, but in some cases it's insufficient.

*spectrace* will help us in such a situation:

```clj
user=> (require '[spectrace.trace :as trace])
nil
user=> (trace/traces ed)
[[{:spec (clojure.spec.alpha/merge
          (clojure.spec.alpha/keys :req-un [:user/x])
          (clojure.spec.alpha/keys :req-un [:user/y])),
   :path [:x], :val {:x :a}, :in [:x], :spec-name :user/m}
  {:spec (clojure.spec.alpha/keys :req-un [:user/x]),
   :path [:x], :val {:x :a}, :in [:x]}
  {:spec clojure.core/integer?, :path [], :val :a, :in [], :spec-name :user/x}]
 [{:spec (clojure.spec.alpha/merge
          (clojure.spec.alpha/keys :req-un [:user/x])
          (clojure.spec.alpha/keys :req-un [:user/y])),
   :path [], :val {:x :a}, :in [], :spec-name :user/m}
  {:spec (clojure.spec.alpha/keys :req-un [:user/y]),
   :path [], :val {:x :a}, :in []}
  {:spec (clojure.core/fn [%] (clojure.core/contains? % :y)),
   :path [], :val {:x :a}, :in []}]]                                                            
user=> 
```

It traces and enumerates all the specs involved in the spec error and makes it easy to build more structured error messages.

## License

Copyright © 2017 Shogo Ohta

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

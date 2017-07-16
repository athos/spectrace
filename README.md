# spectrace 
[![Clojars Project](https://img.shields.io/clojars/v/spectrace.svg)](https://clojars.org/spectrace)
[![CircleCI](https://circleci.com/gh/athos/spectrace.svg?style=shield)](https://circleci.com/gh/athos/spectrace)
[![codecov](https://codecov.io/gh/athos/spectrace/branch/master/graph/badge.svg)](https://codecov.io/gh/athos/spectrace)

clojure.spec (spec.alpha) library aiming to be a fundamental tool for analyzing spec errors

## Installation

Add the following to your `:dependencies`:

[![Clojars Project](https://clojars.org/spectrace/latest-version.svg)](http://clojars.org/spectrace)

## Why and how to use it?

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

Although this might be useful enough as it is to make simple error messages, it's not sufficient in some cases due to the difficulty of extracting more useful information from it.

*spectrace* will help us in such a situation:

```clj
user=> (require '[spectrace.core :as strace])
nil
user=> (strace/traces ed)
[[{:spec
   (clojure.spec.alpha/merge
     (clojure.spec.alpha/keys :req-un [:user/x])
     (clojure.spec.alpha/keys :req-un [:user/y])),
   :path [:x],
   :val {:x :a},
   :in [:x]
   :trail [],
   :spec-name :user/m}
  {:spec (clojure.spec.alpha/keys :req-un [:user/x]),
   :path [:x],
   :val {:x :a},
   :in [:x],
   :trail [0]}
  {:spec clojure.core/integer?, :path [], :val :a, :in [], :trail [0 :x], :spec-name :user/x}]
 [{:spec
   (clojure.spec.alpha/merge
     (clojure.spec.alpha/keys :req-un [:user/x])
     (clojure.spec.alpha/keys :req-un [:user/y])),
   :path [],
   :val {:x :a},
   :in [],
   :trail [],
   :spec-name :user/m}
  {:spec (clojure.spec.alpha/keys :req-un [:user/y]),
   :path [],
   :val {:x :a},
   :in [],
   :trail [1]}
  {:spec (clojure.core/fn [%] (clojure.core/contains? % :y)),
   :path [],
   :val {:x :a},
   :in [],
   :trail [1]}]]
user=> 
```

It traces and enumerates all the specs involved in the spec error and makes it easy to build more structured error messages.
For example, [_Pinpointer_](https://github.com/athos/Pinpointer), a pretty-printing reporter for spec errors, is using spectrace to achieve its high quality error reporting.

## Issues to be addressed

The following is a list of spec.alpha issues that I'm aware are to be addressed before a final release of spectrace:

- [[CLJ-2068] s/explain of evaluated predicate yields :s/unknown](https://dev.clojure.org/jira/browse/CLJ-2068)
- [[CLJ-2143] The result of s/form for s/keys\* is different from the original form](https://dev.clojure.org/jira/browse/CLJ-2143)
- [[CLJ-2152] clojure.spec: s/& has a broken form](https://dev.clojure.org/jira/browse/CLJ-2152)
- [[CLJ-2168] clojure.spec: :pred in explain for coll-of does't use resolved symbols](https://dev.clojure.org/jira/browse/CLJ-2168)
- [[CLJ-2176] s/tuple explain-data :pred problems](https://dev.clojure.org/jira/browse/CLJ-2176)
- [[CLJ-2177] s/keys explain-data :pred problem](https://dev.clojure.org/jira/browse/CLJ-2177)
- [[CLJ-2178] s/& explain-data :pred problem](https://dev.clojure.org/jira/browse/CLJ-2178)

## License

Copyright © 2017 Shogo Ohta

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

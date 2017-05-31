(defproject spectrace "0.1.0-SNAPSHOT"
  :description "A utility library to trace and enumerate specs involved in a given spec error"
  :url "https://github.com/athos/spectrace"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.562"]
                 [org.clojure/spec.alpha "0.1.123"]]
  :plugins [[lein-eftest "0.3.1"]]
  :aliases {"test" ["eftest"]})

(defproject spectrace "0.1.0-SNAPSHOT"
  :description "clojure.spec (spec.alpha) library aiming to be a fundamental tool for analyzing spec errors"
  :url "https://github.com/athos/spectrace"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :test-paths ["test/clj" "test/cljc"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha18" :scope "provided"]
                 [org.clojure/clojurescript "1.9.908" :scope "provided"]
                 [specium "0.1.0-SNAPSHOT"]]

  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/out/test.js"
                                   :output-dir "target/out"
                                   :main spectrace.runner
                                   :optimizations :none}}
                       {:id "node-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/node_out/test.js"
                                   :output-dir "target/node_out"
                                   :main spectrace.runner
                                   :optimizations :none
                                   :target :nodejs}}]}

  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.10.0-alpha1"]
                             [orchestra "2017.07.04-1"]]
              :plugins [[lein-cloverage "1.0.9"]
                        [lein-doo "0.1.7"]
                        [lein-eftest "0.3.1"]]}}

  :eftest {:report eftest.report.pretty/report}

  :aliases {"test-clj" ["eftest"]
            "test-cljs" ["do" #_["test-cljs-none" "once"]
                              ["test-cljs-node" "once"]]
            "test-cljs-none" ["doo" "phantom" "test"]
            "test-cljs-node" ["doo" "node" "node-test"]
            "test-all" ["do" ["test-clj"] ["test-cljs"]]})

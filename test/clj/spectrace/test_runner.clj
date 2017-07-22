(ns spectrace.test-runner
  (:refer-clojure :exclude [test])
  (:require [clojure.spec.test.alpha :as t]
            [eftest.runner :as eftest]
            [eftest.report.pretty :as pretty]
            spectrace.specs.core))

(defn do-test []
  (t/instrument)
  (try
    (eftest/run-tests (eftest/find-tests "test/cljc")
                      {:report pretty/report})
    (finally
      (t/unstrument))))

(defn -main []
  (do-test)
  (shutdown-agents))

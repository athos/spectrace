(ns spectrace.test-runner
  (:require [orchestra.spec.test :as st]
            [eftest.runner :as eftest]
            [eftest.report.pretty :as pretty]
            spectrace.specs.core))

(defn do-test []
  (st/instrument)
  (try
    (eftest/run-tests (eftest/find-tests "test/cljc")
                      {:report pretty/report})
    (finally
      (st/unstrument))))

(defn -main []
  (do-test)
  (shutdown-agents))

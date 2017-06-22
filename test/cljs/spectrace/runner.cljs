(ns spectrace.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            spectrace.trace-test))

(doo-tests 'spectrace.trace-test)

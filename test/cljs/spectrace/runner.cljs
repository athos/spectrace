(ns spectrace.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            spectrace.core-test))

(doo-tests 'spectrace.core-test)

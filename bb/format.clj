(ns format
  "This module provides a task to check and fix code formatting using [cljstyle](https://github.com/greglook/cljstyle)."
  (:require
    [babashka.process :refer [sh]]
    [clojure.term.colors :as c]))


;; Method to handle formatter tasks.
(defmulti format-code (fn [& args] (first args)))


;; Check the style of all Clojure files.
(defmethod format-code "check" [& _]
  (println (c/blue "Checking Clojure file style..."))
  (-> (sh "clj -M:format -m cljstyle.main check") :out println))


;; Fix the style of all Clojure files.
(defmethod format-code "fix" [& _]
  (println (c/blue "Formatting all Clojure files..."))
  (-> (sh "clj -M:format -m cljstyle.main fix") :out println))


;; Default handler to catch unknown formatter commands.
(defmethod format-code :default [& args]
  (println (c/yellow "Unknown build target:") (c/red (first args))))


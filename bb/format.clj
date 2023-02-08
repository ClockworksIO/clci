(ns format
  "This module provides a task to check and fix code formatting using [cljstyle](https://github.com/greglook/cljstyle)."
  (:require
    [babashka.process :refer [sh]]
    [clci.term :refer [with-c]]))


;; Method to handle formatter tasks.
(defmulti format-code (fn [& args] (first args)))


;; Check the style of all Clojure files.
(defmethod format-code "check" [& _]
  (println (with-c :blue "Checking Clojure file style..."))
  (-> (sh "clj -M:format -m cljstyle.main check") :out println))


;; Fix the style of all Clojure files.
(defmethod format-code "fix" [& _]
  (println (with-c :blue "Formatting all Clojure files..."))
  (-> (sh "clj -M:format -m cljstyle.main fix") :out println))


;; Default handler to catch unknown formatter commands.
(defmethod format-code :default [& args]
  (println (with-c :yellow "Unknown build target:") (with-c :red (first args))))


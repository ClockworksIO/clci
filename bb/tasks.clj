(ns tasks
  "This module defines various tasks that are available to work with the `clci` package.
  This includes perform tests and building the library."
  (:require
    [babashka.process :refer [shell sh]]
    [clojure.edn :as edn]
    [clojure.term.colors :as c]))


(defn run-tests
  "Run all available tests.
  Invokes the kaocha test runner."
  []
  (-> (sh "clj -M:test") :out println))


(defn nrepl
  "Start a nREPL server."
  []
  (println "Starting nREPL server on localhost...")
  (-> (sh "clj -M:nREPL -m nrepl.cmdline") :out println))



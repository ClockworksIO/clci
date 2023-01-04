(ns tasks
  "This module defines various tasks that are available to work with the `clci` package.
  This includes perform tests and building the library."
  (:require
    [babashka.process :refer [shell sh]]
    [clci.git-hooks-utils :refer [changed-files]]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.term.colors :as c]
    [format :as fmt]))


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


(defn foo
  ""

  [])

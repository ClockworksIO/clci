(ns clci.tools.mkdocs
  "This module provides tooling to use mkdocs as an external tool to
  build a beautiful documentation."
  (:require
    [babashka.cli :as cli]
    [clci.proc-wrapper :as pw]))


(def cli-options
  ""
  {:spec
   {:build	{:coerce :boolean :desc "Build the documentation (static html)"}
    :serve  {:coerce :boolean :desc "Serve the documentation on localhost including hot reloading."}
    :help   {:coerce :boolean :desc "Show help."}}})


(defn- print-help
  "Print help for the docs task."
  []
  (println "Run mkdocs to build the documentation.\n")
  (println (cli/format-opts cli-options)))


(defmulti docs-impl (fn [& args] (first args)))


;; Build the documentation
(defmethod docs-impl :build [& _]
  (pw/wrap-process ["mkdocs build"]))


;; Run a server on localhost to serve the documentation
(defmethod docs-impl :serve [& _]
  (pw/wrap-process ["mkdocs serve"]))


;; Default handler to catch invalid arguments, print help
(defmethod docs-impl :default [& _]
  (print-help))


(defn docs!
  "Run mkdocs."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:build opts) (docs-impl :build)
    (:serve opts) (docs-impl :serve)
    :else (docs-impl :help)))

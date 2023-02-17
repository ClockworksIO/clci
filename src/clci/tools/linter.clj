(ns clci.tools.linter
  "Implementation of the linter. Uses the `clj-kondo` library
	and defines some sane abstractions how to use it from a bb task."
  (:require
   [babashka.cli :as cli]
   [clj-kondo.core :as clj-kondo]))

(defn- lint-impl
  "Run kondo to lint the code.
  Raises a non zero exit code if any errors are detected. Takes an optional
  argument `fail-on-warnings?`. If true the function also raises a non zero
  exit code when kondo detects any warnings."
  [opts]
  (let [{:keys [summary] :as results} (clj-kondo/run! {:lint ["src"]})]
    (clj-kondo/print! results)
    (cond
      ;;
      (and (:fail-on-warnings opts) (pos? (:warning summary)))
      (throw (ex-info "Linter failed!" {:babashka/exit 1}))
      ;;
      (and (not (:no-fail opts)) (pos? (:error summary)))
      (throw (ex-info "Linter failed!" {:babashka/exit 1}))
      ;;
      :else
      nil)))

(def cli-options
  "Available cli options for linter/kondo."
  {:spec
   {:fail-on-warnings	{:coerce :boolean :desc "Set to true if you would like the linter to fail when a warning is detected."}
    :no-fail      		{:coerce :boolean :desc "Set to true when the linter should not return a non zero exit code at any errors."}
    :help         		{:coerce :boolean :desc "Show help."}}})

(defn- print-help
  "Print help for the lint task."
  []
  (println "Run kondo to lint the codebase.\n")
  (println (cli/format-opts cli-options)))

(defn lint
  "Run kondo to lint the codebase."
  {:org.babashka/cli cli-options}
  [opts]
  (if (:help opts)
    (print-help)
    (lint-impl opts)))

(ns clci.tools.linesofcode
  "Implementation of the tool to get the lines of code"
  (:require
    [babashka.cli :as cli]
    [clci.repo :refer [get-paths]]
    [clci.term :as c]
    [com.mjdowney.loc :as loc]))


(def cli-options
  "Available cli options for carve."
  {:spec
   {:report       {:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :silent       {:coerce :boolean :desc "Set to true if you would like to not write anything to stdout when running the tool i.e. in a CI environment."}
    :help         {:coerce :boolean :desc "Show help."}}})


(defn- print-help
  "Print help for the carve task."
  []
  (println "Get the lines of code of the project's code base.\n")
  (println (cli/format-opts cli-options)))


(defn- lines-of-code-impl
  [opts]
  (let [write-report?   (:report opts)
        silent?         (:silent opts)
        report 					(-> (loc/breakdown {:root (get-paths)})
                        with-out-str)]
    (when-not silent?
      (print report))
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))


(defn lines-of-code
  "Run linesofcode-bb to get the lines of code."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:help opts) 	(print-help)
    :else 			 	(lines-of-code-impl opts)))



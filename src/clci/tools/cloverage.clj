(ns clci.tools.cloverage
  "Tool wrapper of cloverave to get the test coverage of the code."
  (:require
   [babashka.cli :as cli]
   [clci.term :as c]
   [babashka.process :refer [sh]]))

(def cli-options
  ""
  {:spec
   {:src-path   {:coerce :string :require true :desc "Path to the directory of the code."}
    :test-path	{:coerce :string :require true :desc "Path to the directory of the tests."}
    :report   	{:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :silent   	{:coerce :boolean :desc "Set to true if you would like to not write anything to stdout when running in a CI environment."}
    :help  			{:coerce :boolean :desc "Show help."}}})

(defn- print-help
  "Print help for the task."
  []
  (println "Calculate code test coverage using 'cloverage'.\n")
  (println (cli/format-opts cli-options)))

(defn- cloverage-impl
  "Implementation of the cloverage execution."
  [opts]
  (let [write-report?   (:report opts)
        silent?         (:silent opts)
        src-path        (:src-path opts)
        test-path       (:test-path opts)
        result   				(sh
                      {:out :string :err :string}
                      (format "clj -M:coverage -m cloverage.coverage -p %s -s %s --text" src-path test-path))
        report          (-> result :out)]
    ;; write report to stdout if not supressed
    (when-not silent?
      (print report))
    ;; write a report if requested
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))

(defn cloverage
  ""
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:help opts)  (print-help)
    :else 				(cloverage-impl opts)))
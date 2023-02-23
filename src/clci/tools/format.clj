(ns clci.tools.format
  "This module provides a task to check and fix code formatting using [cljfmt](https://github.com/weavejester/cljfmt)."
  (:require
    [babashka.cli :as cli]
    [babashka.process :refer [sh]]
    [clci.term :as c]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]))


(def cli-options
  ""
  {:spec
   {:check  	{:coerce :boolean :desc "Run the formatter in check mode. Files are not changed."}
    :no-fail  {:coerce :boolean :desc "Set to true to in combination with the --check option to not return a non 0 exit code."}
    :report   {:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :silent   {:coerce :boolean :desc "Set to true if you would like to not write anything to stdout when running in a CI environment."}
    :fix  		{:coerce :boolean :desc "Run the formatter and automatically fix all style violations."}
    :help  		{:coerce :boolean :desc "Show help."}}})


(defn- print-help
  "Print help for the format task."
  []
  (println "Run the code formatter.\n")
  (println (cli/format-opts cli-options)))


;; Method to handle formatter tasks.
(defmulti format-code-impl (fn [& args] (first args)))


;; Check the style of all Clojure files.
(defmethod format-code-impl :check [_ opts]
  (let [write-report?   (:report opts)
        silent?         (:silent opts)
        no-fail?        (:no-fail opts)
        _								(when-not silent?
                   (println (c/blue "Checking Clojure file style...")))
        result   				(sh {:out :string :err :string} "clojure -M:format -m cljstyle.main check")
        failure?				(not= 0 (:exit result))
        report          (-> result :err str/split-lines)]
    ;; write report to stdout if not supressed
    (when-not silent?
      (pprint report))
    ;; write a report if requested
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))
    ;; maybe block next step in workflow?
    (when (and failure? (not no-fail?))
      (System/exit 1))))


;; Fix the style of all Clojure files.
(defmethod format-code-impl :fix [_ opts]
  (println (c/blue "Checking Clojure file style..."))
  (let [result   				(sh {:out :string :err :string} "clojure -M:format -m cljstyle.main fix")
        failure?				(not= 0 (:exit result))
        report          (-> result :err str/split-lines)]
    (pprint report)
    (when failure?
      (System/exit 1))))


;; Default handler to catch unknown formatter commands.
(defmethod format-code-impl :default [& _]
  (print-help))


(defn format!
  "Implementation wrapper to format clojure code."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:check opts) (format-code-impl :check opts)
    (:fix opts) (format-code-impl :fix opts)
    :else (format-code-impl :help)))

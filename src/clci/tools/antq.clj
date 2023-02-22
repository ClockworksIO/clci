(ns clci.tools.antq
  ""
  (:require
   [babashka.cli :as cli]
   [clci.term :as c]
   [babashka.process :refer [sh]]
   [clci.util :refer []]))

(def cli-options
  ""
  {:spec
   {:check  	{:coerce :boolean :desc "Run antq in check mode. Dependencies are not updated."}
    :report   {:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :silent   {:coerce :boolean :desc "Set to true if you would like to not write anything to stdout when running in a CI environment."}
    :upgrade	{:coerce :boolean :desc "Run antq and upgrade all outdated dependencies automatically."}
    :help  		{:coerce :boolean :desc "Show help."}}})

(defn- print-help
  "Print help for the antq task."
  []
  (println "Check for outdated dependencies.\n")
  (println (cli/format-opts cli-options)))

;; Method to handle antq task.
(defmulti find-outdated-impl (fn [& args] (first args)))

;; Check dependencies for outdated - don't upgrade!.
(defmethod find-outdated-impl :check [_ opts]
  (let [write-report?   (:report opts)
        silent?         (:silent opts)
        result   				(sh {:out :string :err :string} "clj -M:outdated -m antq.core")
        report          (-> result :out)]
    ;; write report to stdout if not supressed
    (when-not silent?
      (print report))
    ;; write a report if requested
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))

;; Check dependencies for outdated and upgrade.
(defmethod find-outdated-impl :upgrade [_ opts]
  (let [write-report?   (:report opts)
        silent?         (:silent opts)
        result   				(sh {:out :string :err :string} "clj -M:outdated -m antq.core --upgrade --force --download")
        report          (-> result :out)]
    ;; write report to stdout if not supressed
    (when-not silent?
      (print report))
    ;; write a report if requested
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))

;; Default handler to catch unknown formatter commands.
(defmethod find-outdated-impl :default [& _]
  (print-help))

(defn find-outdated-dependencies
  ""
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:check opts) (find-outdated-impl :check opts)
    (:upgrade opts) (find-outdated-impl :upgrade opts)
    :else (find-outdated-impl :help)))
 (ns clci.tools.carve
   "Implementation of the carve wrapper. This module uses the
  `carve` library to identify and optionally remove unused variables
  and dead code from a project. The module defines an api how to
  use carve in an easy way from a bb task."
   (:require
     [babashka.cli :as cli]
     [carve.api :as api]
     [clci.repo :refer [get-paths]]
     [clci.term :as c]
     [clojure.edn :as edn]
     [clojure.pprint :refer [pprint]]))


(def cli-options
  "Available cli options for carve."
  {:spec
   {:check        {:coerce :boolean :desc "Set to true to only check and not transform code."}
    :report       {:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :no-fail      {:coerce :boolean :desc "Set to true to in combination with the --report option to only write a report but do not return a non 0 exit code."}
    :silent       {:coerce :boolean :desc "Set to true if you would like to not write anythin to stdout when running carve i.e. in a CI environment."}
    :fix          {:coerce :boolean :desc "Set to true to transform the code."}
    :interactive  {:coerce :boolean :desc "Interactive mode: ask what to do with an unused var."}
    :help         {:coerce :boolean :desc "Show help."}}})


(defn- print-help
  "Print help for the carve task."
  []
  (println "Run carve to remove unused vars.\n")
  (println (cli/format-opts cli-options)))


(defmulti carve-impl (fn [& args] (first args)))


;; Run carve in check mode - the code won't be changed but a
;; report may be created and a non zero exit code may be returned
;; i.e. to stop further execution of a pipeline
(defmethod carve-impl :check [_ opts]
  (println (c/blue "Running Carve in check mode"))
  (let [report          (-> (api/carve! {:paths (get-paths) :report {:format :edn} :dry-run true})
                            with-out-str
                            edn/read-string)
        incident-cnt    (count report)
        failure?        (seq report)
        write-report?   (:report opts)
        silent?         (:silent opts)
        no-fail?        (:no-fail opts)]
    ;; print report to stdout
    (when (and failure? (not silent?))
      (println (c/yellow "Carve found") (c/magenta incident-cnt) (c/yellow "issues:"))
      (pprint report))
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))
    ;; maybe block next step in workflow?
    (when (and failure? (not no-fail?))
      (System/exit 1))))


;; Run carve in fix mode - use with care!
(defmethod carve-impl :fix [_]
  (println (c/blue "Running Carve in fix mode"))
  (let [report          (-> (api/carve! {:paths (get-paths) :report {:format :edn}}))]
    (println (c/yellow "Carve fixed the following issues:"))
    (pprint report)))


;; Run carve in interactive mode - useful during development on a local machine
(defmethod carve-impl :interactive [_]
  (println (c/blue "Running Carve in interactive mode"))
  (api/carve! {:paths (get-paths) :interactive true}))


;; Print help
(defmethod carve-impl :help [_]
  (print-help))


(defn carve!
  "Run carve to identify and optionally remove unused vars."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:check opts) (carve-impl :check opts)
    (:fix opts) (carve-impl :fix opts)
    (:interactive opts) (carve-impl :interactive)
    :else (carve-impl :help)))

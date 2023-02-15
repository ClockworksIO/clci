(ns clci.carve
  ""
  (:require
    [babashka.cli :as cli]
    [carve.api :as api]
    [clci.term :refer [with-c]]
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]))



(defn get-paths
  "TODO: combine with utils from monorepo!"
  []
  ["src"])


(def carve-cli-options
  "Available cli options for carve."
  {:spec
   {:check        {:coerce :boolean :desc "Set to true to only check and not transform code."}
    :report       {:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :no-fail      {:coerce :boolean :desc "Set to true to in combination with the --report option to only write a report but do not return a non 0 exit code."}
    :silent       {:coerce :boolean :desc "Set to true if you would like to not write anythin to stdout when running carve i.e. in a CI environment."}
    :fix          {:coerce :boolean :desc "Set to true to transform the code."}
    :interactive  {:coerce :boolean :desc "Interactive mode: ask what to do with an unused var."}
    :help         {:coerce :boolean :desc "Show help."}}})


(defn carve-print-help
  ""
  []
  (println "Run carve to remove unused vars.\n")
  (println (cli/format-opts carve-cli-options)))



(defmulti carve-impl (fn [& args] (first args)))


;; Run carve in check mode - the code won't be changed but a
;; report may be created and a non zero exit code may be returned
;; i.e. to stop further execution of a pipeline
(defmethod carve-impl :check [_ opts]
  (println (with-c :blue "Running Carve in check mode"))
  (let [report          (-> (api/carve! {:paths (get-paths) :report {:format :edn} :dry-run true})
                            with-out-str
                            edn/read-string)
        failure?        (not (empty? report))
        write-report?   (:report opts)
        silent?         (:silent opts)
        no-fail?        (:no-fail opts)]
    ;; print report to stdout
    (when (and failure? (not silent?))
      (println (with-c :yellow "Carve found the following issues:"))
      (pprint report))
    (when write-report?
      (println (with-c :magenta "REPORT NOT IMPLEMENTED YET!")))
    ;; maybe block next step in workflow?
    (when (and failure? (not no-fail?))
      (System/exit 1))))


;; Run carve in fix mode - use with care!
(defmethod carve-impl :fix [_ opts]
  (println (with-c :blue "Running Carve in fix mode"))
  (let [report          (-> (api/carve! {:paths (get-paths) :report {:format :edn}}))]
    (println (with-c :yellow "Carve fixed the following issues:"))
    (pprint report)))


;; Run carve in interactive mode - useful during development on a local machine
(defmethod carve-impl :interactive [_]
  (println (with-c :blue "Running Carve in interactive mode"))
  (api/carve! {:paths (get-paths) :interactive true}))


;; Print help
(defmethod carve-impl :help [_]
  (carve-print-help))


(defn carve
  "Run carve to identify and optionally remove unused vars."
  {:org.babashka/cli carve-cli-options}
  [opts]
  (cond
    (:check opts) (carve-impl :check opts)
    (:fix opts) (carve-impl :fix opts)
    (:interactive opts) (carve-impl :interactive)
    :else (carve-impl :help)))

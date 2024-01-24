(ns clci.core
  "This module provides the means to install clci in any project and run
  a task to manage that project."
  (:require
    [babashka.cli :as cli]
    [clci.assistant.products :refer [run-add-product-assistant]]
    [clci.assistant.setup :refer [run-setup-assistant]]
    [clci.term :refer [yellow blue red green]]
    [clci.tools.core :as tools]
    [clci.workflow.runner :refer [valid-trigger? run-trigger]]
    [clci.workflow.workflow :refer [workflow-successful?]]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]))


(defn- print-workflow-job-history
  [key history]
  (let [job-failed? (fn [story] (:failure story))]
    (println "")
    (println "Job log for Workflow " (yellow (name key)) ":")
    (doseq [story history]
      (println (format "Job %s %s" (get-in story [:context :job :ref]) (if (job-failed? story) (red "failed") (green "successful")))))
    (println "")))



(defn run-workflow-trigger
  "Run all workflows started by the given trigger."
  [args]
  (let [trigger   (get-in args [:opts :trigger])
        verbose?  (get-in args [:opts :verbose])
        debug?    (get-in args [:opts :debug])]
    (if (valid-trigger? trigger)
      (try
        (let [report (run-trigger trigger)]
          (cond
            ;; with verbose logging show a summary of all workflows and their jobs
            verbose?
            (doseq [[key log] (:log report)]
              (println "Running workflow" (yellow (name key)) (green "successful \u2713"))
              (print-workflow-job-history key (:history (last log))))
            ;; with debugging show the full runner report
            debug?
            (doseq [[key log] (:log report)]
              (println "Running workflow" (yellow (name key)) (green "successful \u2713"))
              (println "Full log of workflow " (yellow (name key)))
              (pprint log))))
        (catch clojure.lang.ExceptionInfo exinf
          (case (:reason (ex-data exinf))
            :no-workflow-found-for-trigger
            (println (yellow "No workflow found for trigger ") (blue trigger))
            :workflows-not-spec-conform
            (println (red "\u2A2F Workflow does not conform to spec! "))
            :workflow-failure
            (when (or verbose? debug?)
              (println (yellow "At least one workflow failed:"))
              (doseq [[key log] (:log (ex-data exinf))]
                (if (workflow-successful? log)
                  (println "Running workflow" (yellow (name key)) (green "successful \u2713"))
                  (do
                    (println "Running workflow" (yellow (name key)) (red "not successful \u2A2F"))
                    (when verbose?
                      (print-workflow-job-history key (:history (last log))))
                    (when debug?
                      (println "Full log of workflow " (yellow (name key)))
                      (pprint log)))))))
          (System/exit 1))
        (catch Exception e
          (println (red "Unknown Error"))
          (println (yellow (.getMessage e)))
          (System/exit 1)))
      (println (yellow trigger) (red "is not a valid trigger!")))))


(def build-in-jobs
  "All jobs  build-in to clci."
  {"lines-of-code"      tools/lines-of-code-job
   "format"             tools/format-clojure-job
   "lint"               tools/lint-clojure-job
   "outdated"           tools/outdated-job
   "update-changelog"   tools/update-changelog-job})


;; TODO: only a stubb
(defn get-custom-jobs
  ""
  []
  {})


(defn- dispatch-job
  "Dispatch the given `job`.
   Looks in the build-in jobs and custom jobs of the repo and
   if the given job was found, run it."
  [job args]
  (let [all-jobs (merge build-in-jobs (get-custom-jobs))]
    (if (contains? all-jobs job)
      ((get-in all-jobs [job :fn]) args)
      (println (red "Job with name") (yellow job) (red "not found. Did you spell the job correctly?")))))


(defn run-job
  "Run a job based on command line input."
  [args]
  (let [job (get-in args [:opts :job])]
    (dispatch-job job args)))


(defn list-jobs
  "List all available jobs."
  [_]
  (println "The following jobs are available:")
  (doseq [[name job] (merge build-in-jobs (get-custom-jobs))]
    (println (format "%-36s%s" name (:description job)))))


(defn setup-repo
  "Setup the current repo for clci."
  [args]
  (run-setup-assistant args))


(defn add-product
  "Add a new product to the repo using the product assistant."
  [_]
  (run-add-product-assistant))


(defn- handle-main-input-errors
  "Handle errors caused by invalid input passed to the main function.
  Show a useful error message to the user."
  [err]
  (case (:reason (ex-data err))
    :invalid-initial-version (println (red "\u2A2F") " The initial version must follow the Semantic Versioning Specification!")
    :no-workflow-found-for-trigger (println (yellow "No workflow found for the trigger"))
    :workflows-not-spec-conform (println (red "Some workflows do not conform to spec!") err)
    (println (red "unknown error") "\n" err)))


(defn print-help
  "Print the help."
  []
  (println
    (str/trim
      "
Usage: clci <subcommand> <options>

setup                                   Setup the repository for clci.
       
setup git-hooks [options...]            Setup git hooks to trigger clci workflows.

product add                             Add a product using the Product Assistant.
       
run trigger <trigger> [options...]      Run the given <trigger> to execute the relevant workflows.
  available options:
  --verbose                             Set to write a log of the triggered workflows to stdout.
  --debug                               Set to write extensive logging about the triggered workflows to stdout.
       
run job <job> [options...]              Run the Job <job> with optional arguments [options].

list jobs                               List all available jobs.
")))


(defn -main
  "Main entry point, run to install clci in a repo."
  [& _args]
  (try
    (cli/dispatch
      [{:cmds ["setup"]
        :fn   setup-repo}
       {:cmds ["setup" "git-hooks"]
        :fn   tools/setup-git-hooks
        :spec {:pre-commit     {:coerce :boolean :desc "Set to create a hook for a pre-commit workflow."}
               :commit-msg     {:coerce :boolean :desc "Set to create a hook for a commit-msg workflow."}}}
       {:cmds ["product" "add"]
        :fn   add-product}
       {:cmds ["run" "trigger"]
        :fn   run-workflow-trigger
        :args->opts [:trigger]
        :spec {:trigger {:coerce :keyword
                         :required true
                         :desc "Specify the trigger."}
               :verbose {:coerce :boolean
                         :desc  "Set to show a summary of the workflow execution."}
               :debug   {:coerce :boolean
                         :desc  "Set to show the full log of all workflows."}}}
       {:cmds ["run" "job"]
        :fn run-job
        :args->opts [:job]}
       {:cmds ["list" "jobs"]
        :fn list-jobs}
       {:cmds ["release"]
        :fn tools/release!}
       {:cmds []
        :fn (fn [{:keys [opts]}]
              (if (:version opts)
                (println "VERSION") ; TODO: add version print
                (print-help)))}]

      *command-line-args*)
    (catch clojure.lang.ExceptionInfo ex (handle-main-input-errors ex))))


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

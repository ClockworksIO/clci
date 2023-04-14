(ns clci.core
  "This module provides the means to install clci in any project and run
  a task to manage that project."
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [clci.repo :as rp]
    [clci.term :refer [yellow blue red green]]
    [clci.tools.core :as tools]
    [clci.util :refer [pretty-spit!]]
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
  {"lines-of-code"  tools/lines-of-code-job
   "format"         tools/format-clojure-job
   "lint"           tools/lint-clojure-job
   "outdated"       tools/outdated-job})


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


(defn install-in-repo
  "Install clci in the current repo."
  [args]
  (println (blue "Setting up clci in the existing repo."))
  ;; check if required files exist, if not create them with empty content
  (when-not (fs/exists? "bb.edn")
    (pretty-spit! "bb.edn" {}))
  (when-not (fs/exists? "deps.edn")
    (pretty-spit! "deps.edn" {}))
  (when-not (fs/exists? "repo.edn")
    (pretty-spit! "repo.edn" {}))
  (let [opts (:opts args)
        bb-edn    (rp/read-bb)
        deps-edn  (rp/read-deps)]
    ;; setup the repo.edn minimal base
    ;; update bb.edn with the clci task
    (-> bb-edn
        ;; add
        (assoc-in [:tasks 'clci] '{:doc  "Run clci."
                                   :requires    ([clci.core :as clci])
                                   :task (exec 'clci/clci)})
        (rp/write-bb!))
    (-> (rp/repo-base (select-keys opts [:scm :scm-provider :scm-repo-name :scm-repo-owner]))
        (rp/with-single-product (select-keys opts [:initial-version]))
        (rp/write-repo!))))


(defn- handle-main-input-errors
  "Handle errors caused by invalid input passed to the main function.
  Show a useful error message to the user."
  [err]
  (case (:reason (ex-data err))
    :invalid-initial-version (println (red "\u2A2F") " The initial version must follow the Semantic Versioning Specification!")
    :no-workflow-found-for-trigger (println (yellow "No workflow found for the trigger"))
    :workflows-not-spec-conform (println (red "Some workflows do not conform to spec!") err)
    (println (red "unkwnon error") "\n" err)))


(defn print-help
  "Print the help."
  []
  (println
    (str/trim
      "
Usage: clci <subcommand> <options>

install                                 Install clci in this repository.
       
setup git-hooks [options...]            Setup git hooks to trigger clci workflows.
       
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
      [{:cmds ["install"]
        :fn install-in-repo
        :spec
        {:scm             {:coerce :keyword :require true :desc "Name of the scm used. Only supports 'git' right now."}
         :scm-provider    {:coerce :keyword :require true :desc "Provider of the repository service. Only supports 'github' right now."}
         :scm-repo-name   {:coerce :string :require true :desc "Name of the repository."}
         :scm-repo-owner  {:coerce :string :require true :desc "Owner (i.e. user or organization) of the repository."}
         :single-repo     {:coerce :boolean :desc "Set to true if the repository contains only a single product at its root. This will setup the repo.edn configuration accordingly."}
         :initial-version {:coerce :string :desc "Optional version used to set for the product. Only applicable in combination with the `--single-repo` option."}}}
       {:cmds ["run" "trigger"]
        :fn run-workflow-trigger
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
       {:cmds ["setup" "git-hooks"]
        :fn   tools/setup-git-hooks
        :spec {:pre-commit     {:coerce :boolean :desc "Set to create a hook for a pre-commit workflow."}
               :commit-msg     {:coerce :boolean :desc "Set to create a hook for a commit-msg workflow."}}}
       {:cmds ["release"]
        :fn tools/release!}
       {:cmds []
        :fn (fn [{:keys [opts]}]
              (if (:version opts)
                (println "VERSION") ; TODO: add version print
                (print-help)))}]

      *command-line-args*)
    (catch clojure.lang.ExceptionInfo ex (handle-main-input-errors ex))))


(comment
  "bb -m clci.core install --scm git --scm-provider github --scm-repo-name example --scm-repo-owner superman --single-repo")


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

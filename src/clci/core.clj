(ns clci.core
  "This module provides the means to install clci in any project and run
  a task to manage that project."
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [clci.repo :as rp]
    [clci.term :refer [yellow blue red]]
    [clci.tools.core :as tools]
    [clci.util :refer [pretty-spit!]]
    [clci.workflow.runner :refer [valid-trigger? run-trigger]]
    [clojure.string :as str]))



(defn run-workflow-trigger
  "Run all workflows started by the given trigger."
  [args]
  (let [trigger (get-in args [:opts :trigger])]
    (if (valid-trigger? trigger)
      (run-trigger trigger)
      (println (yellow trigger) (red "is not a valid trigger!")))))


(def build-in-jobs
  "All jobs  build-in to clci."
  {"lines-of-code" tools/lines-of-code-job})


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
    :invalid-initial-version (println (red "\u2A2F") " The initial version must follow the Semantic Versioning Specification!")))


(defn print-help
  "Print the help."
  []
  (println
    (str/trim
      "
Usage: clci <subcommand> <options>

install                       Install clci in this repository
run trigger <trigger>         Run the given <trigger> to execute the relevant workflows.
run job <job> [options...]    Run the Job <job> with optional arguments [options].
list jobs                     List all available jobs.
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
        :args->opts [:trigger]}
       {:cmds ["run" "job"]
        :fn run-job
        :args->opts [:job]}
       {:cmds ["list" "jobs"]
        :fn list-jobs}
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

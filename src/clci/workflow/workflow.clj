(ns clci.workflow.workflow
  (:require
    [babashka.fs :as fs]
    ;; [clci.actions :as actions]
    ;; [clci.repo :as rp]
    [clci.util :refer [any find-first slurp-edn]]
    [clci.workflow.action]
    ;; [cljc.java-time.local-date :as ld]
    ;; [clojure.core.async :as a]
    [clojure.spec.alpha :as s]))



;; The name of the workflow - short and descriptive
(s/def :clci.workflow/name string?)


;; A key to uniquely identify the workflow
(s/def :clci.workflow/key keyword?)


;; A description of what the workflow does
(s/def :clci.workflow/description string?)

(s/def :clci.workflow.trigger/ident #{:git-commit-msg :git-pre-commit :manual})


;; The trigger that starts the workflow
(s/def :clci.workflow/trigger (s/coll-of :clci.workflow.trigger/ident))


(s/def :clci.workflow.filter/ident seq?)


;; Optional filter for the workflow. If not present, scope :repository is assumed
(s/def :clci.workflow/filter :clci.workflow.filter/ident)


;; Optional flag to disable the workflow
(s/def :clci.workflow/disabled? boolean?)


;; An optional reference to the job within the workflow.
;; Required to access the output of a specific job in the workflow.
(s/def :clci.workflow.job/ref keyword?)


;; The action that is executed by this job
(s/def :clci.workflow.job/action :clci.action/ident)


;; The inputs passed to the job
(s/def :clci.workflow.job/inputs map?)


;; A job within a workflow
(s/def :clci.workflow.job/ident
  (s/keys :req-un [:clci.workflow.job/action]
          :opt-un [:clci.workflow.job/ref
                   :clci.workflow.job/inputs]))


;; Collection with all jobs run in the workflow. Jobs are run in order top to bottom.
(s/def :clci.workflow/jobs
  (s/and
    (s/coll-of :clci.workflow.job/ident)
    #(not (empty? %))))


;; A workflow
(s/def :clci.workflow/ident
  (s/keys :req-un [:clci.workflow/name
                   :clci.workflow/key
                   :clci.workflow/description
                   :clci.workflow/trigger
                   :clci.workflow/jobs]
          :opt-un [:clci.workflow/disabled?
                   :clci.workflow/filter]))


(s/def :clci.runner-log.entry/job :clci.workflow.job/ident)


(s/def :clci.runner-log.entry.context/started-at some?)

(s/def :clci.runner-log.entry.context/triggered-by :clci.workflow/trigger)

(s/def :clci.runner-log.entry.context/ident (s/keys :req-un [:clci.runner-log.entry.context/started-at]))

(s/def :clci.runner-log.entry/context :clci.runner-log.entry.context/ident)


;; The types of messages found in the runner log
(s/def :clci.runner-log.entry/msg-type #{::workflow-start ::job-pre-run ::job-after-run ::error ::failure ::finished})


;; A runner log entry must have at least the key of the workflow and 
;; the type of the message. The rest depends on the message type
(s/def :clci.runner-log.entry/ident
  (s/keys :req-un [:clci.runner-log.entry/msg-type
                   :clci.workflow/key]
          :opt-un [:clci.runner-log.entry/job
                   :clci.runner-log.entry/context]))


;; A workflow runner log is a map where the keys are the key of the workflow
;; and the value is a collection of runner-log entries
(s/def :clci.runner-log/ident (s/map-of keyword? (s/coll-of :clci.runner-log.entry/ident)))


(defn valid-workflow?
  "Predicate to test if the given `workflow` follows the clci workflow specs."
  [workflow]
  (s/valid? :clci.workflow/ident workflow))


(def workflow-dir-path
  "The releative path to the repo's workflow directory."
  "./.clci/workflows")


(defn- resolve-job-action
  "Resolve the action of the given `job`.
   Test if the action of the job is a symbol and if true, resolve the symbol and replace the symbol
   with the resolved data."
  [job]
  (if (symbol? (:action job))
    (assoc job :action @(requiring-resolve (:action job)))
    job))


(defn resolve-workflow-action
  ""
  [workflow]
  (assoc
    workflow
    :jobs
    (mapv resolve-job-action (:jobs workflow))))


(defn get-workflows
  "Get all workflows defined in repo.edn."
  []
  (if (fs/exists? workflow-dir-path)
    (->> (fs/list-dir workflow-dir-path "*.edn")
         (map (comp slurp-edn #(format "%s/%s" workflow-dir-path %) fs/file-name))
         (map resolve-workflow-action))
    []))


(defn find-workflows-by-trigger
  "Find all workflows started by a specific trigger.
   Takes a collection of `workflows` and a `trigger. Returns a collection with all
   workflows started by the trigger."
  [workflows trigger]
  (filter
    (fn [wf]
      (some #{trigger} (get wf :trigger [])))
    workflows))


(defn remove-disabled-workflows
  "Remove all `workflows` that are currently disabled."
  [workflows]
  (remove :disabled? workflows))


(defn workflow-successful?
  "Test if the workflow did run without failure.
   Takes either
   - the log of a single workflow run as vector of events
   or
   - the full `runner-log` and the `workflow-key` of the workflow.
   Returns true if if the workflow did run without a failure, false else."
  ([single-log]
   (as-> single-log $
         (find-first (fn [m] (= :clci.workflow.runner/finished (:msg-type m))) $)
         (some? $)))
  ([runner-log workflow-key]
   (as-> runner-log $
         (get $ workflow-key '())
         (find-first (fn [m] (= :clci.workflow.runner/finished (:msg-type m))) $)
         (some? $))))


(defn get-workflow-history
  "Get only the final history for a workflow run.
   Takes the full `runner-log` and the `workflow-key` of the workflow.
   Returns nil if no such workflow exists."
  [runner-log workflow-key]
  (as-> runner-log $
        (get $ workflow-key '())
        (find-first (fn [m]
                      (or
                        (= :clci.workflow.runner/finished (:msg-type m))
                        (= :clci.workflow.runner/failure (:msg-type m))))
                    $)
        (:history $)))

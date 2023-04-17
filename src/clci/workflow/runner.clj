(ns clci.workflow.runner
  (:require
    [clci.util.core :refer [any find-first]]
    [clci.workflow.reporter :refer [default-reporter]]
    [clci.workflow.workflow :refer [find-workflows-by-trigger remove-disabled-workflows get-workflows valid-workflow?]]
    [cljc.java-time.local-date :as ld]
    [clojure.core.async :as a]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))



(defn create-job-context
  "Create a new job context to run jobs in a workflow."
  [{:keys [trigger]}]
  {:started-at   (ld/now)
   :triggered-by  trigger})


(defn create-runner-state
  "Create the inital state for the runner.
   Takes the `workflow` the runner should run."
  [workflow]
  {:current-job-idx   0
   :failure           false
   :total-jobs        (count (:jobs workflow))
   :history           []})


(defn job-output?
  "Takes the input value `v` of a job and checks if the value is a named reference
   to the output of another job.
   A job output reference is a keyword following the format `:!job.[a-zA-Z\\-]/[a-zA-Z0-9\\-]`,
   i.e. :!job.job-ref/some-output"
  [v]
  (and
    (keyword? v)
    (some? (re-matches #"!job.[a-zA-Z0-9\-]+" (or (namespace v) "")))))


(defn get-job-history
  "Get the history of a specific job identified by the job's `ref` in the `history`"
  [history ref]
  (find-first (fn [h-entry] (= ref (get-in h-entry [:context :job :ref]))) history))


(defn get-job-output-value
  "Get the value of the output of a specific job.
   Takes the workflow's `history`, the `job-ref` of the job in question and the
   `output-name` of which the value should be fetched."
  [history job-ref output-name]
  (-> (get-job-history history job-ref)
      :outputs
      (get output-name)))


(defn derive-inputs
  "Derive the input for the current job run in a workflow.
   Resolves input placeholders to get outputs from previous jobs.
   Takes the current `job` and the `history` of the workflow.
   Tests if the inputs of the current job reference any outputs and if that
   is true, then get the output value from the execution history."
  [job history]
  (as-> (:inputs job) $
        (map
          (fn [[k v]]
            (if (job-output? v)
              [k (get-job-output-value history (-> v (namespace) (str/split #"\.") (get 1) (keyword)) (-> v (name) (keyword)))]
              [k v]))
          $)
        (into (hash-map) $)))


(defn workflow-runner
  "Workflow runner - runs all jobs in a linear order.
   Takes the `workflow`, the `trigger` that invoked the runner and a channel `ch` to
   communicate the status of the runner. Also takes the following optional keyword arguments:
   | key                | Description                                                                    |
   | ------------------ | ------------------------------------------------------------------------------ |
   | `:debug?`          | When true, communicate addition debugging messages. Boolean, defaults to false |
   
   A Runner starts a new thread that iterates over the workflow jobs and executes them in order. Each
   job gets it own context which is derived from the runner's state. This includes the outputs of
   previously run jobs of the workflow.
   The runner does not return any data, instead it uses the channel given as an argument to send status
   about the runner status, the execution of each job and a full history of the workflow exection once
   the workflow is finished.
   If a job's run returns a failure, the workflow is stopped and information about the failure is put
   on the channel. If an error occures during workflow execution, information about the error is put on
   the channel."
  [workflow trigger ch & {:keys [debug?] :or {debug? false}}]
  (let [initial-job-context     (create-job-context {:trigger trigger})
        inital-state            (create-runner-state workflow)

        ;; test if the workflow was completly executed
        completed?          (fn [s] (and (not (:failure s)) (>= (:current-job-idx s) (:total-jobs s))))
        ;; test if there was a failure during workflow execution
        failure?            (fn [s] (:failure s))
        ;; get the job that has to be run next
        get-job 						(fn [s] (get-in workflow [:jobs (:current-job-idx s)]))
        ;; derive the execution context for the current job
        derive-job-context  (fn [job s]
                              (assoc initial-job-context
                                     :job (-> (get-job s)
                                              (select-keys [:ref])
                                              (assoc :inputs (derive-inputs job (:history s))))))
        ;; run the job, produces a history entry
        run-job             (fn [job context]
                              (when debug?
                                (a/>!! ch {:msg-type ::job-pre-run
                                           :key      (:key workflow)
                                           :job      job
                                           :context  context}))
                              (let [result          ((get-in job [:action :fn]) context)
                                    history-entry   {:context (select-keys context [:job :inputs])
                                                     :outputs  (:outputs result)
                                                     :failure (:failure result)}]
                                (a/>!! ch {:msg-type      ::job-after-run
                                           :key           (:key workflow)
                                           :history-entry history-entry})
                                history-entry))]
    (a/thread
      (a/>!! ch {:msg-type      ::workflow-start
                 :key           (:key workflow)})
      (try
        (loop [state    inital-state]
          (let [job   (get-job state)]
            (cond
              ;; runner finished
              (completed? state)
              (do
                (a/>!! ch
                       {:msg-type ::finished
                        :key      (:key workflow)
                        :failure  (:failure state)
                        :history  (:history state)})
                (a/close! ch))
              ;; last job failed
              (failure? state)
              (do
                (a/>!! ch
                       {:msg-type ::failure
                        :key      (:key workflow)
                        :history  (:history state)})
                (a/close! ch))
              ;; continue with next job
              :else
              (recur
                (as-> state $
                      (update-in $ [:history] conj (run-job job (derive-job-context job state)))
                      (update $ :current-job-idx inc)
                      (assoc $ :failure (-> $ :history last :failure)))))))
        (catch Exception ex
          (a/>!! ch {:msg-type        ::error
                     :key             (:key workflow)
                     :msg             "Workflow execution failed with an error!"
                     :exception-msg   (.getMessage ex)})
          (a/close! ch))))))


(defn run-workflow
  "Execute a workflow using a runner.
   Takes the `workflow` and the `trigger` that invoked the workflow.
   Returns a workflow log."
  [workflow trigger & {:keys [debug?] :or {debug? false}}]
  (let [ch      (a/chan)
        _       (workflow-runner workflow trigger ch {:debug? debug?})
        log  (atom [])]
    (loop []
      (let [msg (a/<!! ch)]
        (when-not (nil? msg)
          (swap! log conj msg)
          (recur))))
    {(:key workflow) @log}))


(defn run-workflows
  "Execute all given workflows in their own runner.
   Takes the `workflows` and the `trigger` that invoked the workflows.
   Returns a workflow log."
  [workflows trigger & {:keys [debug?] :or {debug? false} :as opts}]
  (let [cs      (repeatedly (count workflows) a/chan)
        _       (doseq [c cs w workflows] (workflow-runner w trigger c {:debug? debug?}))
        m-ch    (a/merge cs)
        log     (atom [])]
    (loop []
      (let [msg (a/<!! m-ch)]
        (when-not (nil? msg)
          (swap! log conj msg)
          (recur))))
    (group-by (fn [msg] (get msg :key :unknown)) @log)))


(defn run-trigger-impl
  "Implementation of `run-trigger`.
   Takes the `trigger` and all available `workflows`."
  [trigger workflows reporter & {:keys [debug?] :or {debug? false} :as opts}]
  (let [relevant-workflows (-> workflows
                               (find-workflows-by-trigger trigger)
                               (remove-disabled-workflows))]
    ;; relevant-workflows
    (cond
      (empty? relevant-workflows)
      (throw (ex-info (format "No workflows found for trigger %s" trigger) {:reason :no-workflow-found-for-trigger}))

      (any #(not (valid-workflow? %)) workflows)
      (throw (ex-info "Some workflows do not follow the correct specs!" {:reason :workflows-not-spec-conform}))

      :else
      (let [report    (reporter (run-workflows relevant-workflows trigger opts))]
        (if (:failure? report)
          (throw (ex-info "At least one workflow finished with a failure!" {:reason :workflow-failure
                                                                            :log    (:log report)}))
          report)))))


(defn valid-trigger?
  "Test if the given `trigger` is a valid trigger."
  [trigger]
  (s/valid? :clci.workflow.trigger/ident trigger))


(defn run-trigger
  "Run a `trigger` to execute all workflows listeing to the trigger.
   Takes the `trigger` and an optional `reporter`."
  ([trigger]
   (run-trigger trigger default-reporter))
  ([trigger reporter]
   (run-trigger-impl trigger (get-workflows) reporter)))


(defn run-job
  "Run a single Job.
   Used to run a single Action as Job without creating an explicit workflow.
   Takes the `action` that is run, a `name` for the job, the identifying `key`,
   and the `inputs` for the job.
   Returns a workflow log."
  [action name key inputs reporter]
  (let [workflow 	{:name          (format "Adhoc - Job %s" name)
                   :key           key
                   :description   "Adhoc Workflow - Run an adhoc Job."
                   :trigger       [:manual]
                   :disabled?     false
                   :jobs
                   [{:ref     key
                     :action  action
                     :inputs  inputs}]}]
    (reporter (run-workflow workflow :manual))))


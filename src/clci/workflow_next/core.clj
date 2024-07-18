(ns clci.workflow-next.core
  ""
  (:require
    [clci.util.core :refer [find-first-index]]
    [clojure.core.async :as a :refer [go-loop]]
    [clojure.pprint :refer [pprint]])
  (:import
    java.time.LocalDateTime
    java.time.format.DateTimeFormatter))



;; (def example-action
;;   "An example Action that increments an integer by one."
;;   {:name            "Random Integer"
;;    :key             :random-integer
;;    :description     "Create a random integer and writes it to the context."
;;    :scopes          [:repository :product]
;;    :impure?         false
;;    :fn              (fn [ctx]
;;                       {:outputs {:number (inc (get-in ctx [:job :inputs :number]))}
;;                        :failure false})
;;    :inputs          {:number  {:type          :integer
;;                                :description   "The integer that gets incremented."
;;                                :required      true}}
;;    :outputs         {:number  {:type        :integer
;;                                :description "The number created by the action."}}})


(def date-time-formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))


(defn date-time-now
  []
  (.format (LocalDateTime/now) date-time-formatter))


(defn job
  [context get-artefact put-artefact feedback])



(defn executor
  ""
  [])



;; Example Data for Repl development


(def products'
  [{:root "pwa" :key :pwa :release-prefix "pwa" :version "1.2.3"}
   {:root "mobile" :key :mobile :release-prefix "mobile" :version "0.15.23"}
   {:root "backend" :key :backend :release-prefix "backend" :version "1.21.0"}])


(def bricks'
  [{:key :docker :root "docker" :version "0.12.3"}])


(def repo'
  {:scm
   {:type     :git
    :url      "git@github.com:ExampleOrg/example.git"
    :provider {:name :github
               :repo "example"
               :owner "ExampleOrg"}}
   :products products'
   :bricks bricks'})



(def format-job
  ""
  ;; this is a Job
  {:key                   :format-clojure-sources
   :name                  "Format Clojure"
   :description           "Formats all Clojure source files."
   :vars                  [{:src-paths ["src"]}]
   :required-resources    []
   :produced-artefacts    []
   :scope                 :component ; this means either a product or a brick
   :filter                '[AND [COMPONENT IS :product] [PRODUCT KEY IS [:pwa :backend]]]
   ;; alternative filter:
   ;; '[AND [COMPONENT IS :product] [PRODUCT KEY IS :pwa]] 
   ;; '[AND [COMPONENT IS :product] [PRODUCT KEY IS [:pwa :backend]]]
   ;; '[COMPONENT IS [:product :brick]]
   :action               'clci.workflow-next.core/format-action ; 'clci.actions.clojure/format
   :with-side-effects?    true ; this job has side effects because code is formatted in place
   })



(def staged-files-job
  ""
  {:key                   :list-staged-files
   :name                  "List Staged Files"
   :description           "List all files staged for a commit in git."
   :vars                  []
   :required-resources    []
   :produced-artefacts    [{:key :staged-files}]
   :scope                 :repository
   :filter                []
   :action                'clci.workflow-next.core/git-list-staged-files-action; 'clci.actions.git/list-staged-files
   :with-side-effects?    false})


(def add-files-job
  ""
  {:key                   :stage-files
   :name                  "Stage Files"
   :description           "Stages files for the next commit."
   :vars                  []
   :required-resources    [{:key :files :from-job :list-staged-files :scope :repository}]
   :produced-artefacts    []
   :scope                 :repository
   :filter                []
   :action                'clci.workflow-next.core/git-add-files-action; 'clci.actions.git/add
   :with-side-effects?    false})


(def workflow'
  {:key             :example-workflow
   :name            "Example Workflow"
   :description     "Just an example to test this new workflow system."
   :jobs
   [staged-files-job
    format-job
    add-files-job]})



(defn format-action-fn
  [context get-artefact put-artefact send-feedback])


(def format-action
  {:key                       :clci.actions.clojure/format
   :description               "This Action formats all Clojure source files at the specified path using `mvxcvi/cljstyle`."
   :with-side-effects?        true        ; this job has side effects because code is formatted in place.
   :scope                     :component  ; this action is expected to run on a specific component.
   ;; this implies the workdir in which the action is run is the directory where the component is located.
   :vars                      [{:key :src-paths :conform [:vector :string] :required true :description "The path where the Clojure source files are located."}]
   :required-resources        []
   :produced-artefacts        []
   :fn                        'clci.workflow-next.core/format-action-fn})


(def git-list-staged-files-action
  {:key                       :clci.actions.git/list-staged-files
   :description               "This Action lists all files staged for the next commit."
   :with-side-effects?        false
   :scope                     :repository
   :vars                      []
   :required-resources        []
   :produces-artefacts        [{:key :staged-files :conform [:vector :string] :description "All files staged for a commit as a list."}]
   :fn                        'clci.workflow-next.core/list-staged-files-fn})


(def git-add-files-action
  {:key                       :clci.actions.git/add
   :description               "This Action stages files for the nect commit."
   :with-side-effects?        true        ; this job has side effects because code is formatted in place.
   :scope                     :repository
   :vars                      []
   :required-resources        [{:key :files :conform [:vector :string] :description "The files that should be added as a list."}]
   :produced-artefacts        []
   :fn                        'clci.workflow-next.core/add-files-fn})


(defn list-staged-files-fn
  [context get-artefact put-artefact send-feedback]
  (let [dummy-files [".gitignore" "index.md" "src/example/core.clj"]])
  (send-feedback {:msg "Set an artefact value"}))


(defn add-files-fn
  [context get-artefact put-artefact send-feedback])


(comment
  ;; example for a job context
  {:vars  {:src-paths ["src"]}}
  
  )


(def job-queue-limit 256)
(def feedback-ch-buffer-size 16)


(defprotocol IArtefactStore
  ""

  (put-artefact
    [this ctx artefact]
    "Put a new artefact in the store.")

  (get-artefact
    [this artefact] "Get an artefact from the store" .)

  (delete-artefact
    [this artefact]
    "Delete an artefact from the store."))



(defrecord ArtefactStore
  [artefacts]

  IArtefactStore

  (put-artefact [this ctx artefact])


  (get-artefact [this artefact])


  (delete-artefact [this artefact]))


(defn empty-artefact-store
  ""
  []
  (ArtefactStore. (atom {})))


(defn execute-job
  ""
  [context artefact-store send-feedback job]
  (send-feedback {:msg "Running an actual Job!" :job job})
  (requiring-resolve (:action job))
  (let [action (deref (resolve (:action job)))]
    (send-feedback {:msg "Job action" :action action})))


(defn get-next-job
  "Get the next job in the workflow.
   Takes the `workflow` and the key of the current job `current-job-key`.
   Returns the next job in the queue or nil if no more jobs exist after the current."
  [workflow current-job-key]
  (let [current-idx (find-first-index (:jobs workflow) (fn [job] (= current-job-key (:key job))))]
    (get-in workflow [:jobs (inc current-idx)])))


(defn runner
  [workflow artefact-store send-feedback context]
  (let [queue           (a/chan job-queue-limit)]
    (send-feedback {:msg "Starting the runner" :state :starting})
    (send-feedback {:msg "Put first job on the queue" :state :starting})
    ;; get the first job from the workflow and put it at the start of the queue
    (a/put! queue (get-in workflow [:jobs 0]))
    ;; as long as any job is on the queue, execute them one by one
    (go-loop [job (a/<! queue)]
      (try
        ;; execute the job
        (execute-job context artefact-store send-feedback job)
        ;; if there is yet another job left from the workflow, add it to the queue.
        ;; otherwise execution is finished.
        (if-let [next-job (get-next-job workflow (:key job))]
          (a/put! queue next-job)
          (send-feedback {:msg "No jobs remaining." :state :finished}))
        (catch Exception err
          ;; better be save and catch all errors
          (send-feedback {:msg "Oh Snap! Desaster has struck!" :state :error :trace err})))
      (recur (a/<! queue)))))


(defn run-workflow
  [workflow artefact-store collector]
  (let [initial-context   {:execution   (random-uuid)
                           :started-at  (date-time-now)
                           :workflow    (:key workflow)}
        feedback          (a/chan feedback-ch-buffer-size)
        send-feedback   (fn [m] (a/put! feedback (merge {:workflow (:key workflow)} m)))]
    (runner workflow artefact-store send-feedback initial-context)
    (collector feedback)))


(comment
  ;; initialize an empty artefact store
  (def artefact-store (empty-artefact-store))
  ;; dummy collector function
  (def collector (fn [ch]
                   (go-loop [msg (a/<! ch)]
                     (try
                       (pprint msg)
                       (catch Exception e (println "Oh Noes something went terribly wrong!")))
                     (recur (a/<! ch)))))
  ;; run the workflow
  (run-workflow workflow' artefact-store collector) 
  
  )





(deref (resolve 'clci.workflow-next.core/format-action))

(ns clci.workflow-next.core
  ""
  (:require
    [clci.util.core :refer [find-first find-first-index]]
    [clci.util.dev :refer [document document-spec]]
    [clojure.core.async :as a :refer [go-loop]]
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as spec])
  (:import
    java.time.LocalDateTime
    java.time.format.DateTimeFormatter))


(def date-time-formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))


(defn date-time-now
  []
  (.format (LocalDateTime/now) date-time-formatter))



(document
  [:spec :action/fn]
  {}
  ;; Each Action defines a function that does the actual work.
  ;; This function must always conform to the following signtature:
  (fn [context get-resource put-artefact feedback])
  ;;
  ;; Where:
  ;;
  ;; **context** 
  ;; Is a map that provides contectual information to the Action function.
  ;; In many cases this context can be an empty map.
  ;;
  ;; **get-resource**
  ;; Is a function allows an Action function to get a specific resource,
  ;; Its signature is `(fn [key] -> Something)` where the `key` points to a
  ;; specific required resource as specified in the Job declaraion.
  ;;
  ;; **put-artefact**
  ;;
  ;; **feedback**
  ;; A function that sends feedback from a running workflow execution
  ;; upstream to any subscriber (i.e. a file logger).
  ;; The function has the signature `(fn [m]) -> nil` with `m` being a
  ;; map following the .
  ;;
  )



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
   :required-resources    [{:key :src-paths :static ["src"]}]
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
   :required-resources    []
   :produced-artefacts    [{:key :staged-files :type :edn}]
   :scope                 :repository
   :filter                []
   :action                'clci.workflow-next.core/git-list-staged-files-action; 'clci.actions.git/list-staged-files
   :with-side-effects?    false})


(def add-files-job
  ""
  {:key                   :stage-files
   :name                  "Stage Files"
   :description           "Stages files for the next commit."
   :required-resources    [{:key :files :artefact {:job :list-staged-files :key :staged-files}}]
   ;; [{:key :staged-files :from-job :list-staged-files :scope :repository}]
   :produced-artefacts    []
   :scope                 :repository
   :filter                []
   :action                'clci.workflow-next.core/git-add-files-action; 'clci.actions.git/add
   :with-side-effects?    false})


;; TODO: This explanation is no longer valid!
(comment
  ;; the `:required-resources` attribute of a job follows the form
  [:type :key :job :product]
  ;;; examples
  ;; Use a static value, in this case a vector with two strings
  [:static ["src" "test"]]
  ;; Get the artefact identified by the key `:test-report` produced by the job
  ;; `:run-unit-tests` for the product `:backend`
  [:artefact :test-report :run-unit-tests :backend]
  ;; Get the artefact identified by the key `:staged-files` produced by the job
  ;; `:staged-files` for the whole repository
  [:artefact :staged-files :staged-files]
  ;; Get the artefact identified by the key `:report` produced by the job
  ;; `:lint` for all products.
  ;; In this case the artefact will be a map where the keys identify the products.
  [:artefact :report :lint :ALL]
  )


(def workflow'
  {:key             :example-workflow
   :name            "Example Workflow"
   :description     "Just an example to test this new workflow system."
   :jobs
   [staged-files-job
    format-job
    add-files-job]})



(defn format-action-fn
  [context get-resource put-artefact send-feedback])


(def format-action
  {:key                       :clci.actions.clojure/format
   :description               "This Action formats all Clojure source files at the specified path using `mvxcvi/cljstyle`."
   :with-side-effects?        true        ; this job has side effects because code is formatted in place.
   :scope                     :component  ; this action is expected to run on a specific component.
   ;; this implies the workdir in which the action is run is the directory where the component is located.
   :vars                      [{:key :src-paths :conform [:vector :string] :required true :description "The path where the Clojure source files are located."}]
   :produced-artefacts        []
   :fn                        'clci.workflow-next.core/format-action-fn})


(def git-list-staged-files-action
  {:key                       :clci.actions.git/list-staged-files
   :description               "This Action lists all files staged for the next commit."
   :with-side-effects?        false
   :scope                     :repository
   :vars                      []
   :produces-artefacts        [{:key :staged-files :conform [:vector :string] :description "All files staged for a commit as a list."}]
   :fn                        'clci.workflow-next.core/list-staged-files-fn})


(def git-add-files-action
  {:key                       :clci.actions.git/add
   :description               "This Action stages files for the nect commit."
   :with-side-effects?        true        ; this job has side effects because code is formatted in place.
   :scope                     :repository
   :vars                      [{:key :files :conform [:vector :string] :description "The files that should be added as a list."}]
   :produced-artefacts        []
   :fn                        'clci.workflow-next.core/add-files-fn})


(defn list-staged-files-fn
  [context get-resource put-artefact send-feedback]
  (let [dummy-files [".gitignore" "index.md" "src/example/core.clj"]]
    (send-feedback {:msg "Set an artefact value" :level :debug})
    (put-artefact :staged-files dummy-files)))


(defn add-files-fn
  [context get-resource put-artefact send-feedback])


(comment
  ;; example for a job context
  {:vars  {:src-paths ["src"]}}
  
  )


(def job-queue-limit 256)
(def feedback-ch-buffer-size 16)


;; Specs for an Artefact ;;;;

(spec/def :artefact.data/content-type string?)
(spec/def :artefact.data/content (spec/or :text string? :binary bytes?))

(spec/def :artefact/id uuid?)
(spec/def :artefact/key keyword?)
(spec/def :artefact/workflow keyword?)
(spec/def :artefact/execution uuid?)
(spec/def :artefact/job keyword?)
(spec/def :artefact/scope #{:product :brick :component :repository})
(spec/def :artefact/product keyword?)


(spec/def :artefact/artefact
  (spec/keys :req-un [:artefact.data/content-type
                      :artefact.data/content]))


(spec/def :artefact/ident
  (spec/keys :req-un [:artefact/id
                      :artefact/key
                      :artefact/workflow
                      :artefact/execution
                      :artefact/job]
             :opt-un [:artefact/scope
                      :artefact/product]))


(defn mk-artefact
  "Create a new Artefact.
   Takes the raw `artefact` as map following the `:artefact/ident` spec and
   the following keyword args:
   
   | key         | description                                                                     |
   | ----------- | ------------------------------------------------------------------------------- |
   | :id         | The key of the workflow to which the artefact belongs                           |
   | :key        | They key identifying the produced artefact as defined in the underlying Action. |
   | :workflow   | The key of the workflow to which the artefact belongs.                          |
   | :execution  | The execution of the workflow that produced the artefact.                       |
   | :job        | The key of the Job that produced the artefact.                                  |"
  [artefact {:keys [workflow execution job key id scope product] :or {scope :repository}}]
  {:id        id
   :artefact  artefact
   :workflow  workflow
   :execution execution
   :job       job
   :key       key
   :scope     scope
   :product   product})


(comment
  (mk-artefact
    {:content-type  "text/plain"
     :content       "This is just an example for an artefact with simple text content."}
    {:id          #uuid "93b56292-a23a-4fa7-91bc-a1bec1bc952d"
     :key         :example-output
     :workflow    :example-workflow
     :execution   #uuid "619ddb3d-3e28-4a1a-936c-8926eeef3aa5"
     :job         :an-example-job})
  )


;; (defprotocol IQueue
;;   ""
;;   (put [this e] "Put a new element on the queue. Element is added at the end of the queue.")
;;   (pop [this] "Remove the first element from the queue and return it.")
;;   (subscribe [this] "Subscribe to the queue. Yields a channel on which new elements are put when popped."))

(defprotocol IArtefactStore
  ""

  (put-artefact
    [this ctx artefact-data]
    "Put a new artefact in the store.
     Must take a context map `ctx` and the artefact itself.
     The context map must have the following keys:
     
     | key         | required? | default | description                                                                    |
     | ----------- | --------- | ------- | ------------------------------------------------------------------------------ |
     | :workflow   |       yes |    --   | The key of the workflow to which the artefact belongs                          |
     | :execution  |       yes |    --   | The execution of the workflow that produced the artefact                       |
     | :job        |       yes |    --   | The key of the Job that produced the artefact                                  |
     | :key        |       yes |    --   | They key identifying the produced artefact as defined in the underlying Action |")

  (get-artefact
    [this id] "Get an artefact from the store using its id." .)

  (get-artefact
    [this key ctx] "Get an artefact from the store using its key." .)

  (delete-artefact
    [this artefact]
    "Delete an artefact from the store."))


(defn match-all-selected?
  "Matches the values of two maps `m1` and `m2` denoted by a collection of `keys`
   using the given `eq?` function.
   Returns true if the values at given keys of both maps are all equal, false else."
  [m1 m2 keys eq?]
  (every? (fn [k] (eq? (get m1 k) (get m2 k))) keys))


(comment
  ;; true
  (match-all-selected?
    {:k1 :a :k2 3 :k3 "U"}
    {:k1 :a :k2 3 :k3 "U"}
    [:k1 :k2 :k3]
    =)
  ;; true
  (match-all-selected?
    {:k1 :a :k2 3 :k3 "U"}
    {:k1 :a :k2 4 :k3 "U"}
    [:k1 :k3]
    =)
  ;; false
  (match-all-selected?
    {:k1 :a :k2 3 :k3 "U"}
    {:k1 :other :k2 4 :k3 "U"}
    [:k1 :k2 :k3]
    =)
  ;; false
  (match-all-selected?
    {:k1 :a :k2 3 :k3 "U"}
    {:k1 :other :k2 4 :k3 "U"}
    [:k1 :k3]
    =)
  )


(defrecord ArtefactStore
  [artefacts]

  IArtefactStore

  (put-artefact
    [_ ctx artefact-data]
    (let [artefact-id               (random-uuid)
          {:keys [workflow execution job key]}  ctx]
      (swap! artefacts conj (mk-artefact
                              artefact-data
                              {:id        artefact-id
                               :key       key
                               :workflow  workflow
                               :execution execution
                               :job       job}))
      artefact-id))


  (get-artefact
    [_ a-id]
    (->> artefacts
         deref
         (filter (fn [{:keys [id]}] (= a-id id)))
         first))


  (get-artefact
    [_ a-key ctx]
    (->> artefacts
         deref
         (filter (fn [m]
                   (and
                     (= (:key m) a-key)
                     (match-all-selected? m ctx [:workflow :execution :job] =))))
         first))


  (delete-artefact
    [this artefact]
    (throw (ex-info "Not implemented yet!" {}))))


(document
  {}
  'ArtefactStore
  "")


(defn empty-artefact-store
  "Create an empty Artefact Store."
  []
  (ArtefactStore. (atom [])))


(comment
  "Create an artefact store and add and retrieve some artefacts."
  (def my-artefact-store (empty-artefact-store))
  (def workflow-key :example-workflow)
  (def execution (random-uuid))
  (def artefact-key :my-artefact)
  (def job-a-key :job-a)
  (def job-b-key :job-b)
  ;; for the example just use a simple plain text artefact
  (def artefact-data {:content-type  "text/plain"
                      :content       "Gandalf the Grey"})
  ;; manually create the context, this would usually be done automatically
  ;; by the workflow runner.
  (def artefact-ctx {:key       artefact-key
                     :workflow  workflow-key
                     :execution execution
                     :job       job-a-key})
  
  ;; put the example artefact in the store
  (put-artefact my-artefact-store artefact-ctx artefact-data)
  
  ;; manually check if the artefact is present
  (-> my-artefact-store :artefacts deref)
  
  ;; get the artefact using the key and context
  (get-artefact my-artefact-store artefact-key artefact-ctx)
  )



;; Action fn signature: (fn [context get-resource put-artefact send-feedback]) -> nil

;; **Staged**:
;;
;; Job:
;;   :required-resources    []
;;   :produced-artefacts    [{:key :staged-files :type :edn}]
;;
;; Action:
;;   :vars                  []
;;   :produced-artefacts    [{:key :staged-files :conform [:vector :string] :description "All files staged for a commit as a list."}]
;; 
;; 
;; **Format**:
;;
;; Job:
;;   :required-resources    [{:key :src-paths :static "src"}]
;;   :produced-artefacts    []
;;
;; Action:
;;   :vars                  [{:key :src-paths :conform [:vector :string] :required true :description "The path where the Clojure source files are located."}]
;;   :produced-artefacts    [] 
;;
;;
;; **Add**:
;;
;; Job:
;;   :required-resources    [[{:key :files :artefact {:job :list-staged-files :key :staged-files}}]]
;;   :produced-artefacts    []
;;
;; Action:
;;   :vars                  [{:key :files :conform [:vector :string] :description "The files that should be added as a list."}]
;;   :produced-artefacts    []
;;


(defn execute-job
  ""
  [context artefact-store send-feedback job]
  (send-feedback {:msg "Running Job" :job (:key job)})
  (requiring-resolve (:action job))
  (let [action              (deref (resolve (:action job)))
        artefact-ctx-base   {:workflow  (:workflow context)
                             :execution (:execution context)}
        ;; this function resolves a resource requested by an action.
        ;; it can handle both static resources and artefact produced by another job.
        get-resource        (fn [var-key]
                              (let [res (find-first (fn [{:keys [key]}] (= key var-key)) (:required-resources job))]
                                (cond
                                  ;; resource is static
                                  (contains? res :static)
                                  (:static res)
                                  ;; resource referes to an artefact produced earlier
                                  (contains? res :artefact)
                                  (get-artefact
                                    artefact-store
                                    (get-in res [:artefact :key])
                                    (assoc artefact-ctx-base :job (get-in res [:artefact :job])))
                                  ;; unhandled resource type, always yield nil
                                  )))
        ;; this function stores an artefact into the artefact store
        put-artefact!       (fn [key data]
                              (put-artefact artefact-store (assoc artefact-ctx-base :key key :job (:key job)) data))]
    (doseq [v (:vars action)]
      (send-feedback {:msg "Variable" :job (:key job) :var-key (:key v) :var-value (get-resource (:key v)) :level :debug}))
    (requiring-resolve (:fn action))
    (send-feedback {:msg "Action FN" :fn (deref (resolve (:fn action))) :level :debug})
    ((deref (resolve (:fn action))) {} get-resource put-artefact! send-feedback)))


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


(def severity-levels
  "The severity levels of a feedback message.
   Severity is ordered in vector from high to low."
  [:error :warning :info :debug])


(defn- severity-greater-or-equal?
  "Predicate to test if the given severity `a` is higher or equal than
   the given severity `b`."
  [a b]
  (<=
    (or (find-first-index severity-levels #(= % a)) 2)
    (or (find-first-index severity-levels #(= % b)) 1)))


(comment 
  (severity-greater-or-equal? :info :info)  ; -> true
  (severity-greater-or-equal? :error :info) ; -> true
  (severity-greater-or-equal? :debug :info) ; -> false
  (severity-greater-or-equal? :foo :bar)    ; -> false
  )


(defn- or-info
  "Either returns the severity level `:debug` if the given
   `debug?` argument is true or alternatively returns the default severity
   level `:info`."
  [debug?]
  (if debug?
    :debug
    :info))


(defn send-or-ignore
  "Send a message over the feedback channel or ignore it depending on the logging level.
   Takes a message as map `m`, the minimal severity level to be sent over the channel `min-level`,
   and a `send-fn` that sends the message over the channel.
   If the severity level of the message is lower than the minimum severity, the message is droped."
  [m min-level send-fn]
  (when (severity-greater-or-equal? (get m :level :info) min-level)
    (send-fn m)))


(defn run-workflow
  [workflow artefact-store collector & {:keys [debug?] :or {debug? false}}]
  (let [initial-context   {:execution   (random-uuid)
                           :started-at  (date-time-now)
                           :workflow    (:key workflow)}
        feedback          (a/chan feedback-ch-buffer-size)
        send-feedback   (fn [m]
                          (send-or-ignore
                            (merge {:workflow (:key workflow)} m)
                            (or-info debug?)
                            #(a/put! feedback %)))]
    (runner workflow artefact-store send-feedback initial-context)
    (collector feedback)))


(def runner-states
  "The possible states a runner can have."
  #{:starting :running :finished :error})


;;
;; Specs for the messages that may be sent from the workflow execution back to a subscriber.
;;

(spec/def :clci.workflow.execution.message/msg string?)
(spec/def :clci.workflow.execution.message/state runner-states)
(spec/def :clci.workflow.execution.message/trace map?)
(spec/def :clci.workflow.execution.message/level (set severity-levels))


(spec/def :clci.workflow.execution/message
  (spec/keys :opt-un [:clci.workflow.execution.message/msg
                      :clci.workflow.execution.message/state
                      :clci.workflow.execution.message/trace
                      :clci.workflow.execution.message/level]))


(document-spec {} :clci.workflow.execution.message/msg "A human readable message.")
(document-spec {} :clci.workflow.execution.message/state "The current state of the runner.")
(document-spec {} :clci.workflow.execution.message/trace "A trace to provide details about an error.")
(document-spec {} :clci.workflow.execution.message/level "Indicates a severeness of message. As default `:info` is assumed if when no explicit level is set.")


(document-spec
  {}
  :clci.workflow.execution/message
  "When a workflow is executed, the runner and the Action functions
   can use a feedback function to put messages onto a channel to be consumed
   by a subscriber. This mechanism allows both debugging and live feedback about
   the current state of the workflow execution. This includes feedback about
   errors that happen during execution that may halt or crash the workflow
   execution in total.")


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
  (run-workflow workflow' artefact-store collector :debug? true) 
  )



;; (deref (resolve 'clci.workflow-next.core/format-action))

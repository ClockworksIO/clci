(ns clci.workflow-next.core
  ""
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [shell]]
   [clci.util.core :refer [find-first find-first-index]]
   [clci.util.dev :refer [document document-spec ToDo]]
   [clci.workflow-next.artefact :refer [empty-artefact-store get-artefact
                                        put-artefact]]
   [clojure.core.async :as a :refer [go-loop]]
   [clojure.pprint :refer [pprint]]
   [clojure.spec.alpha :as spec]
   [clojure.string :as str])
  (:import
   java.time.format.DateTimeFormatter
   java.time.LocalDateTime))


(def date-time-formatter
  "Format the current data and time."
  (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))


(defn date-time-now
  "Get the current date and time and return it as formatted string."
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
   :produced-artefacts        [{:key :staged-files :conform [:vector :string] :description "All files staged for a commit as a list."}]
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



;; (defprotocol IQueue
;;   ""
;;   (put [this e] "Put a new element on the queue. Element is added at the end of the queue.")
;;   (pop [this] "Remove the first element from the queue and return it.")
;;   (subscribe [this] "Subscribe to the queue. Yields a channel on which new elements are put when popped."))




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
  (try 
    (requiring-resolve (:action job))
      (catch java.io.FileNotFoundException _
        (throw (ex-info "The Action refered by the Job does not exist on the classpath." {:cause :action-not-on-classpath :action (:action job)}))))
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



(def format-java-source-files
  ""
  {:key                   :format-java-source-files
   :name                  "Format Java Sources"
   :description           "Format all Java source Files."
   :required-resources    [{:key :src-dirs :static ["/home/tupel/Dev/projects/wholeX/wholesale-platform-utilities/baseline-testing/lib/src"]}
                           ;{:key :src-files :static ["resources/foo/Bar.java" "resources/foo/Doeh.java"]}
                           {:key :jar-path :static "/home/tupel/.local/bin/google-java-format-1.23.0-all-deps.jar"}]
   :produced-artefacts    [{:key :formated-java-files :type :edn}]
   :scope                 :component
   :filter                []
   :action                'clci.workflow-next.core/format-java-source-files-action
   :with-side-effects?    true})


(def format-java-source-files-action
  {:key                       :clci.actions.java/format
   :description               "This Action formats all Java source files at the specified path using `google-java-format`."
   :with-side-effects?        true
   :scope                     :component
   ;; this implies the workdir in which the action is run is the directory where the component is located.
   :vars                      [{:key :src-dirs :conform [:vector :string] :required true :description "A vector of directory paths where Java source files are located."}
                               {:key :src-files :conform [:vector :string] :required true :description "A vector of file paths of Java source files."}
                               {:key :jar-path :conform [:string] :required true :description "The path to the jar file of google-java-format"}]
   :produced-artefacts        []
   :fn                        'clci.workflow-next.core/format-java-with-google-java-format})


(defn java-file?
  "Predicate to test if the file at given path is a java source file."
  [f-path]
  (some? (re-matches #".*\.java" f-path)))


(comment
  (java-file? "foo.java") ;=> true
  (java-file? "./src/foo.java") ;=> true
  (java-file? "/foo/path/oh.java") ;=> true
  (java-file? "foo/ds.fof") ;=> false
  )


(defn list-java-files-recursively
  ""
  [paths]
  (reduce (fn [acc path]
            (let [path (str path)]
            (cond
              (java-file? path) (conj acc path)
              (fs/directory? path) (concat acc (list-java-files-recursively (fs/list-dir path))))))
    []
    paths))

(comment
  (list-java-files-recursively ["/home/tupel/Dev/projects/wholeX/wholesale-platform-utilities/baseline-testing/lib/src"])
  )


; (defn- list-java-files
;   [path]
;   (try
;     (fs/list-dir path "*/**/*.java")
;     (catch java.nio.file.NoSuchFileException ex
;       (throw (ex-info "Not a valid source directory." {:cause :java-source-directory-does-not-exist :path (ex-message ex)})))))

; (fs/list-dir "/home/tupel/Dev/projects/wholeX/wholesale-platform-utilities/baseline-testing/lib/src/" (fn [p] true))


(defn format-java-with-google-java-format
  "Implementation of the format-java-action.
   Shells out to `google-java-format` and formats Java code."
  [context get-resource put-artefact send-feedback]
  (send-feedback {:msg "Running Java format Action." :state :running :level :debug})
  (let [jar-path  (get-resource :jar-path)
        src-dirs  (get-resource :src-dirs)
        src-files (get-resource :src-files)
        extra-source-files (list-java-files-recursively src-dirs)]
    (when-not (fs/exists? jar-path)
      (throw (ex-info "The specified jar file of `google-java-format` does not exist." {:cause :jar-file-does-not-exist :jar-path jar-path})))
    (send-feedback {:msg "extra-source-files: " :extra-source-files extra-source-files :dir (get-resource :src-dirs)})
    (send-feedback {:msg "All files: " :src-files (str/join " " (concat src-files extra-source-files)) :state :running :level :debug})
    (let [proc (-> (shell {:out :string :err :string :continue true} (format "java -jar %s %s" jar-path (str/join " " (concat src-files extra-source-files)))))]
      (send-feedback {:msg "proc: " :proc proc})
      )
    
    
    )
    
  
  ;(send-feedback {:msg "Resource :src-dirs" :resource (get-resource :src-dirs)})
  ;(send-feedback {:msg "Resource :src-files" :resource (get-resource :src-files)})
  ;(send-feedback {:msg "Resource :jar-path" :resource (get-resource :jar-path)})
  )


(-> (shell {:out :string :err :string :continue true :dir "/home/tupel/Dev/projects/wholeX/wholesale-platform-utilities/baseline-testing/lib"} 
      (format "java -jar %s  ./src/main/java/org/example/Library.java" "/home/tupel/.local/bin/google-java-format-1.23.0-all-deps.jar"))
  ;:out
 )

; {:proc #object[java.lang.ProcessImpl 0xfb4af83 "Process[pid=2268235, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessBuilder$NullOutputStream 0x203af096 "java.lang.ProcessBuilder$NullOutputStream@203af096"], 
;  :out "/*\n * This source file was generated by the Gradle 'init' task\n */\npackage org.example;\n\npublic class Library {\n  public boolean someLibraryMethod() {\n    return true;\n  }\n}\n", :err "", :prev nil, 
;  :cmd ["java" "-jar" "/home/tupel/.local/bin/google-java-format-1.23.0-all-deps.jar" "./src/main/java/org/example/Library.java"]}


(def java-workflow
  {:key             :java-workflow
   :name            "Java Workflow"
   :description     "Just an example to test this new workflow system."
   :jobs
   [format-java-source-files]})


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
  (run-workflow java-workflow artefact-store collector :debug? true) 
  )


(ToDo
  {:desc "Implement a new workflow system." 
   :issue "clci-136"}
  [:workflow/specs "Workflow Spec Validation"
   [:todo "Implement a function to check if an Action conforms to spec"]
   [:todo "Implement a function to check if a Job conforms to spec"]
   [:todo "Implement a function to check if a Workflow conforms to spec"]]
  [:workflow/resources "Workflow Resources" 
   [:todo "Check a given resource read from the artefact store conforms to the spec"]
   [:todo "Check a given artefact conforms to spec when writing it to the artefact store"]
   [:todo "Fallback to a var default value when no resource is set for that var"]
  [:workflow/runner
   [:todo "Support running jobs on (single) components"]]
  [:actions "Update build-in Actions to work with the new workflow system"]
   [:doing "Action: create-random-integer-action"]]
)
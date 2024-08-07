(ns clci.workflow-next.artefact
  "This module defines how Artefacts and Artefact Stores work. It also
   provides a default implementation for an Artefact Store."
  (:require
    [clci.util.dev :refer [document document-spec]]
    [clojure.spec.alpha :as spec]))


;;
;; General utilities needed by the module ;;;;
;;

(defn match-all-selected?
  "Matches the values of two maps `m1` and `m2` denoted by a collection of `keys`
   using the given `eq?` function.
   Returns true if the values at given keys of both maps are all equal, false else."
  [m1 m2 keys eq?]
  (every? (fn [k] (eq? (get m1 k) (get m2 k))) keys))


(comment
  "Example for the `match-all-selected?` predicate function."
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


;;
;; Specs for Artefacts ;;;;
;;

(spec/def :clci.artefact.data/content-type string?)
(spec/def :clci.artefact.data/content (spec/or :text string? :binary bytes?))

(spec/def :clci.artefact/id uuid?)
(spec/def :clci.artefact/key keyword?)
(spec/def :clci.artefact/workflow keyword?)
(spec/def :clci.artefact/execution uuid?)
(spec/def :clci.artefact/job keyword?)
(spec/def :clci.artefact/scope #{:product :brick :component :repository})
(spec/def :clci.artefact/component keyword?)


(spec/def :clci.artefact/artefact
  (spec/keys :req-un [:clci.artefact.data/content-type
                      :clci.artefact.data/content]))


(spec/def :clci/artefact
  (spec/keys :req-un [:clci.artefact/id
                      :clci.artefact/key
                      :clci.artefact/workflow
                      :clci.artefact/execution
                      :clci.artefact/job]
             :opt-un [:clci.artefact/scope
                      :clci.artefact/component]))


(document-spec {} :clci.artefact.data/content-type
               "The content type of the Artefact. i.e. 'text/plain'.")


(document-spec {} :clci.artefact.data/content
               "The actual data of the Artefact. Can either be in text format or binary (byte array).")


(document-spec {} :clci.artefact/id
               "The unique id of the Artefact used to identify it.")


(document-spec {} :clci.artefact/key
               "The Artefact key is used to retrieve an Artefact using the declarative specification of Jobs.
   The `:required-resources` section of a Job can refer to an artefact by using the key of the Artefact and
   the key of a specific Job that has produced the Artefact. This is necessary because the id of an
   Artefact is only set when the Artefact is put into the Artefact Store and thus cannot be known
   in the Workflow declaration.")


(document-spec {} :clci.artefact/workflow
               "The key of the Workflow that has produced the Artefact.")


(document-spec {} :clci.artefact/execution
               "The id of the specific execution of the Workflow that has produced the Artefact.")


(document-spec {} :clci.artefact/job
               "The key of the Job that has produced the Artefact.")


(document-spec {} :clci.artefact/scope
               "The scope of the Artefact denotes if the Artefact is specific to 
   any component (product or brick) or in scope of the whole repository.")


(document-spec {} :clci.artefact/component
               "The key of the component to which the Artefact belongs. Only present if the Artefact 
   belongs to a component.")


(document-spec {} :clci.artefact/artefact
               "Subcomponent of `:clci.artefact/ident` encapsulating the data and data content type.")


(document-spec {} :clci/artefact
               "Describes a full Artefact.
   An Artefact is any chunk of data produced by running an Action. This can for example be
   a report of a linter Action, an detailed test protocol of an Action running tests and so on.
   An Artefact is always produced either for the scope of the whole repository or for a
   specific repository component (i.e. a specific product).")



(defn mk-artefact
  "Create a new Artefact.
   Takes the raw `artefact` as map following the `:clci.artefact/ident` spec and
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
  "Example on creating an Artefact."
  (mk-artefact
    {:content-type  "text/plain"
     :content       "This is just an example for an artefact with simple text content."}
    {:id          #uuid "93b56292-a23a-4fa7-91bc-a1bec1bc952d"
     :key         :example-output
     :workflow    :example-workflow
     :execution   #uuid "619ddb3d-3e28-4a1a-936c-8926eeef3aa5"
     :job         :an-example-job})
  )


;;
;; Artefact Store ;;;;
;;

;; This section defines the functional interface of an Artefact Store
;; and implements a default Artefact Store to run workflows.

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
  "The `ArtefactStore` is the default implementation of an Artefact Store used to store and get Artefacts
   when using the local workflow runner.
   All data is stored in memory and not persistet in any way.")



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

(ns clci.workflow-next.job
  "This modules specifies how a Job is delcared."
  (:require
    [clci.util.dev :refer [document-spec]]
    [clci.workflow-next.action :refer [action-scopes]]
    [clojure.spec.alpha :as spec]))


(spec/def :clci.job/key keyword?)
(spec/def :clci.job/name string?)
(spec/def :clci.job/description string?)
(spec/def :clci.job/required-resources vector?)
(spec/def :clci.job/produced-artefacts vector?)
(spec/def :clci.job/scope action-scopes)
(spec/def :clci.job/action symbol?)
(spec/def :clci.job/with-side-effects? boolean?)


(spec/def :clci/job
  (spec/keys
    :req-un [:clci.job/key
             :clci.job/name
             :clci.job/description
             :clci.job/required-resources
             :clci.job/produced-artefacts
             :clci.job/scope
             :clci.job/action
             :clci.job/with-side-effects?]))


(document-spec {} :clci.job/key
               "Key to identify and reference a Job inside a Workflow.")


(document-spec {} :clci.job/name
               "Human readable name of a Job in a Workflow.")


(document-spec {} :clci.job/description
               "Describe what this Job is doing as part of the Workflow.")


(document-spec {} :clci.job/required-resources
               "Declare the resources required by the Job. The resources are
   mapped to the required resources of the Job's Action. As such the
   keys of the required resources of the Job declaration must match the
   keys of the Action.
   The vector must follow this schema:
   `:required-resources    [{:key :src-paths :static [\"src\"]}]`")


(document-spec {} :clci.job/produced-artefacts
               "Declare the Artefacts proiduced by the Job. The artefacts are
   mapped to the vars of the Job's Action. As such the
   keys of the required resources of the Job declaration must match the
   keys of the Action.
   The vector must follow the schema:
   `:produced-artefacts    [{:key :staged-files :type :edn}]`")


(document-spec {} :clci.action/scope
               "The scope on which the Job runs. See the scope documentation of the
   Actions for detailed information.")


(document-spec {} :clci.action/action
               "The Action that is executed by the Job. Referenced by the symbol
   of the Action. The Action must follow thw `:clci/action` spec.")


(document-spec {} :clci.action/with-side-effects
               "Indicates if the Job has side effects. Must be set `true` if any side effects may
   occur when the Job is executed. Must match the Job's Action side effect declaration.")


(document-spec {} :clci/job
               "A Job is executed as a single step of a Workflow.")


(comment
  "Example how to declare a Job:"
  (def format-job
    {:key                   :format-clojure-sources
     :name                  "Format Clojure"
     :description           "Formats all Clojure source files."
     :required-resources    [{:key :src-paths :static ["src"]}]
     :produced-artefacts    []
     :scope                 :component ; this means either a product or a brick
     ;; :filter                '[AND [COMPONENT IS :product] [PRODUCT KEY IS [:pwa :backend]]]
     ;; alternative filter:
     ;; '[AND [COMPONENT IS :product] [PRODUCT KEY IS :pwa]] 
     ;; '[AND [COMPONENT IS :product] [PRODUCT KEY IS [:pwa :backend]]]
     ;; '[COMPONENT IS [:product :brick]]
     :action               'clci.workflow-next.core/format-action ; 'clci.actions.clojure/format
     :with-side-effects?    true ; this job has side effects because code is formatted in place
     })
  )

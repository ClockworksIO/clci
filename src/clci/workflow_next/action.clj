(ns clci.workflow-next.action
  "This module defins how an Action is declared."
  (:require
    [clci.util.dev :refer [document-spec]]
    [clojure.spec.alpha :as spec]))



(def action-scopes
  "The scopes that an Action can use."
  #{:repository :component :brick :product})


(spec/def :clci.action/key keyword?)
(spec/def :clci.action/description string?)
(spec/def :clci.action/with-side-effects? boolean?)
(spec/def :clci.action/scope action-scopes)
(spec/def :clci.action/vars vector?)
(spec/def :clci.action/produced-artefacts vector?)
(spec/def :clci.action/fn symbol?)


(spec/def :clci/action
  (spec/keys :req-un [:clci.action/key
                      :clci.action/description
                      :clci.action/with-side-effects?
                      :clci.action/scope
                      :clci.action/vars
                      :clci.action/produced-artefacts
                      :clci.action/fn]))


(document-spec {} :clci.action/key
               "The key to identify an Action. Used to reference an Action in a Job.")


(document-spec {} :clci.action/description
               "Describe what the Action does.")


(document-spec {} :clci.action/with-side-effects?
               "Indicates if the Action has side effects. Must be set `true` if any side effects may
   occur when the Action is executed.")


(document-spec {} :clci.action/scope
               "The scope denotes in which scope the action may be used.")


(document-spec {} :clci.action/vars
               "Declare the variables needed by the Action. A variable may be resolved either
   to a static value or to the value of an Artefact produced by a Job.
   A variable is declared like
   [{:key :files :conform [:vector :string] :description \"The files that should be added as a list.\"}]")


(document-spec {} :clci.action/produced-artefacts
               "Declare the Artefacts that are produced by the Action.
   Artefacts are declared like
   [{:key :staged-files :conform [:vector :string] :description \"All files staged for a commit as a list.\"}]")


(document-spec {} :clci.action/fn
               "The function which performs the actual work. Must follow the signature:
   `(fn [context get-resource put-artefact send-feedback]) -> nil`")


(document-spec {} :clci/action
               "An Action is a map declaring what the Action requires and produces.")


(comment
  "Example for an Action"
  (def git-list-staged-files-action
    {:key                       :clci.actions.git/list-staged-files
     :description               "This Action lists all files staged for the next commit."
     :with-side-effects?        false
     :scope                     :repository
     :vars                      []
     :produced-artefacts        [{:key :staged-files :conform [:vector :string] :description "All files staged for a commit as a list."}]
     :fn                        'clci.workflow-next.core/list-staged-files-fn})
  )

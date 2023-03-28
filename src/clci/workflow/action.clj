(ns clci.workflow.action
  (:require
    [clojure.spec.alpha :as s]))


;; Indicator if running the action caused a failure or not.
;; If a failure occured, it must contain information about the failure.
(s/def :clci.action.result/failure
  (s/or
    :no-failure  nil?
    :failure     some?))


;; A map with the outputs of the action.
(s/def :clci.action.result/outputs map?)


;; The result of an Action function.
(s/def :clci.action.result/ident
  (s/keys :req-un [:clci.action.result/outputs
                   :clci.action.result/failure]))


;; The name of the Action - short and descriptive
(s/def :clci.action/name string?)


;; A key to uniquely identify the Action
(s/def :clci.action/key keyword?)


;; A description of what the Action does
(s/def :clci.action/description string?)


;; The scope on which the Action can be applied on
(s/def :clci.action/scopes (s/coll-of #{:repository :product}))


;; An indicator if the action has side effects
(s/def :clci.action/impure? boolean?)


;; The function executed by the Action
;; Takes one argument: The execution context `ctx`, with metadata required by the job and
;; and the optional inputs data. Returns a map following the `:clci.action.result/ident` spec.
(s/def :clci.action/fn fn?)
(s/def :clci.action.input/ident map?) ; TODO - implement spec for this
;; The inputs the Action accepts
(s/def :clci.action/inputs
  (s/or
    :no-inputs nil?
    :inputs    (s/every-kv keyword? :clci.action.input/ident)))


(s/def :clci.action.output/ident map?) ; TODO - implement spec for this
;; The outputs the Action produces
(s/def :clci.action/outputs
  (s/or
    :no-outputs  nil?
    :outputs     (s/every-kv keyword? :clci.action.output/ident)))


;; An Action
(s/def :clci.action/ident
  (s/keys :req-un [:clci.action/name
                   :clci.action/key
                   :clci.action/description
                   :clci.action/scopes
                   :clci.action/fn]
          :opt-un [:clci.action/inputs
                   :clci.action/outputs
                   :clci.action/impure?]))



(defn valid-action?
  "Predicate to test if the given `action` follows the clci Action specs."
  [action]
  (s/valid? :clci.action/ident action))

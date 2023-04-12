(ns clci.workflow.reporter
  (:require
    [clci.util :refer [any]]
    [clci.workflow.workflow :refer [get-workflow-history workflow-successful?]]
    [clojure.pprint :refer [pprint]]))



;; (defn default-reporter
;;   "Default reporter for workflow runners.
;;    Takes the workflow log and returns it."
;;   [workflow-log]
;;   (pprint workflow-log)
;;   workflow-log)


(defn default-reporter
  "Produces the default reporter for workflow runners.
   Takes the workflow log and returns it."
  [runner-log]
  ;; (pprint runner-log)
  ;; (println "failure? " (any (fn [[k _]] (not (workflow-successful? runner-log k))) runner-log))
  {;; :failure? (not (workflow-successful? runner-log workflow-key))
   :failure? (any (fn [[k _]] (not (workflow-successful? runner-log k))) runner-log)
   :log      runner-log})

(ns clci.tools.impl
  "Implementations of the ad-hoc actions used by clci tools."
  (:require
    [clci.workflow.workflow :refer [get-workflow-history workflow-successful?]]
    [clojure.string :as str]
    [com.mjdowney.loc :as loc]))



(defn- loc-md-table->map
  "Takes the result of "
  [md]
  (let [lines     (map (fn [l] (mapv str/trim (str/split l #"\|"))) md)]
    (->> (rest (rest lines))
         (map (fn [l]
                {:file (get l 1)
                 :loc (get l 2)
                 :docs (get l 3)
                 :comment-forms (get l 4)
                 :lines (get l 5)}))
         (drop-last))))


(defn lines-of-code-action-edn-reporter
  "Reporter for lines-of-code ad-hoc Action.
   Takes the `workflow-key`.
   Returns a reporter function that either returns the output of the lines of code
   function or a failure."
  [workflow-key]
  (fn [runner-log]
    (if (workflow-successful? runner-log workflow-key)
      (as-> (get-workflow-history runner-log workflow-key) $
            (get-in $ [0 :outputs :report])
            (str/split-lines $)
            (subvec $ 3)
            (loc-md-table->map $))
      (ex-info "Failed to run Action." {}))))


(defn lines-of-code-action-text-reporter
  "Reporter for lines-of-code ad-hoc Action.
   Takes the `workflow-key`.
   Returns a reporter function that either returns the output of the lines of code
   function or a failure."
  [workflow-key]
  (fn [runner-log]
    (if (workflow-successful? runner-log workflow-key)
      (-> (get-workflow-history runner-log workflow-key)
          (get-in [0 :outputs :report]))
      (ex-info "Failed to run Action." {}))))


(defn lines-of-code-action-impl
  "Implementation of the lines-of-code action."
  [ctx]
  (let [paths (get-in ctx [:job :inputs :paths])
        loc-report (with-out-str (loc/breakdown {:root paths}))]
    {:outputs {:report loc-report}
     :failure false}))

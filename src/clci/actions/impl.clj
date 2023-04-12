(ns clci.actions.impl
  "Implementations of the actionsprovided by clci.
   This includes the implementation of ad-hoc actions."
  (:require
    [babashka.process :refer [sh]]
    [carve.api :as api]
    [clci.conventional-commit :refer [valid-commit-msg?]]
    [clci.git :refer [staged-files]]
    [clci.workflow.workflow :refer [get-workflow-history workflow-successful?]]
    [clj-kondo.core :as clj-kondo]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [com.mjdowney.loc :as loc]))


(defn conventional-commit-linter-action-impl
  "Implements the action function to lint a git commit message."
  [_]
  (let [commit-msg (slurp ".git/COMMIT_EDITMSG")
        msg-valid? (valid-commit-msg? commit-msg)]
    {:outputs {:valid? msg-valid?}
     :failure (not msg-valid?)}))



;;
;; Lines of Code - Implementation ;;;
;;

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


(defn git-staged-files-action-impl
  "Implementation to get all staged files for the Staged Files Action."
  [_]
  (try
    {:outputs {:staged (staged-files)}
     :failure false}
    (catch Exception _
      {:outputs {}
       :failure true})))


(defn git-add-files-action-impl
  "Add the specified files to the next commit."
  [ctx]
  (try
    (let [file-paths (get-in ctx [:job :inputs :files])]
      (doseq [file file-paths]
        (sh (format "git add %s" file)))
      {:outputs {}

       :failure false})
    (catch Exception _
      {:outputs {}
       :failure true})))


(defn format-clj-action-reporter
  "A Reporter for the format ad-hoc Action.
   Wrapps the output of the underlying Clojure code formatter and
   returns it in a useful format."
  [workflow-key]
  (fn [runner-log]
    {:failure? (not (workflow-successful? runner-log workflow-key))
     :log      runner-log
     :report   (-> (get-workflow-history runner-log workflow-key)
                   (get-in [0 :outputs :report]))}))


(defn format-clj-action-impl
  "Format clojure files action impl."
  [ctx]
  (try
    (let [fix?        (get-in ctx [:job :inputs :fix] false)
          no-fail?    (get-in ctx [:job :inputs :no-fail] false)
          result      (if fix?
                        (sh {:out :string :err :string} "clojure -M:format -m cljstyle.main fix")
                        (sh {:out :string :err :string} "clojure -M:format -m cljstyle.main check"))
          failure?    (not= 0 (:exit result))
          report      (-> result :err str/split-lines)]
      {:outputs {:report report}
       :failure (and failure? (not no-fail?))})
    (catch Exception _
      {:outputs {}
       :failure true})))


(defn lint-clj-action-reporter
  "A Reporter for the lint ad-hoc Action.
   Wrapps the output of the linter (kondo) and
   returns it in a useful format."
  [workflow-key]
  (fn [runner-log]
    {:failure? (not (workflow-successful? runner-log workflow-key))
     :log      runner-log
     :report   (-> (get-workflow-history runner-log workflow-key)
                   (get-in [0 :outputs :report]))}))


(defn lint-clj-action-impl
  "Lint clojure files action impl."
  [ctx]
  (try
    (let [fail-level      (get-in ctx [:job :inputs :fail-level] :error)
          paths           (get-in ctx [:job :inputs :paths] :error)
          {:keys [summary] :as results} (clj-kondo/run! {:lint paths})
          report          (with-out-str (clj-kondo/print! results))
          with-warnings?  (pos? (:warning summary))
          with-errors?    (pos? (:error summary))]
      (cond
        ;; warnings found and should fail on warnings
        (and (= :warning fail-level) with-warnings?)
        {:outputs {:report report}
         :failure true}
        ;; errors found and should fail on errors
        (and (= :error fail-level) with-errors?)
        {:outputs {:report report}
         :failure true}
        ;; everything good
        :else
        {:outputs {:report report}
         :failure false}))
    (catch Exception _
      {:outputs {}
       :failure true})))


(defn clj-carve-action-impl
  "Run carve in check mode as action."
  [ctx]
  (try
    (let [no-fail?        (get-in ctx [:job :inputs :no-fail] false)
          paths           (get-in ctx [:job :inputs :paths] :error)
          report          (-> (api/carve! {:paths paths :report {:format :edn} :dry-run true})
                              with-out-str
                              edn/read-string)
          failure?        (seq report)]
      {:outputs {:report report
                 :foo failure?}
       :failure (and failure? (not no-fail?))})
    (catch Exception _
      {:outputs {}
       :failure true})))

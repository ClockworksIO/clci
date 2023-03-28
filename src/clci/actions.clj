(ns clci.actions
  (:require
    [clci.conventional-commit :refer [valid-commit-msg?]]
    [clci.term :as c]
    [clci.tools.impl :as timpl]
    [clojure.spec.alpha :as s]))



(def conventional-commit-linter-action
  "An example Action to run the CC linter on git commit messages."
  {:name            "Lint Commit Message: Conventional Commit"
   :key             :lint-git-commit-msg-with-conventional-commit-spec
   :description     "Takes the commit message and test if it follows the Conventional Commit specification."
   :scopes          [:repository]
   :fn              (fn [_]
                      (let [commit-msg (slurp ".git/COMMIT_EDITMSG")
                            msg-valid? (valid-commit-msg? commit-msg)]
                        (if msg-valid?
                          (println (c/green "\u2713") " commit message follows the Conventional Commit specification")
                          (println (c/red "\u2A2F") " commit message does NOT follow the Conventional Commit specification\nAbort commit!"))
                        {:outputs {:valid? msg-valid?}
                         :failure (not msg-valid?)}))
   :inputs          nil
   :outputs         {:valid?  {:type        :boolean
                               :description "True if the commit message follows the Conventional Commit specification."}}})


(def random-integer-action-inputs
  {:maximum {:type          :integer
             :description   "Upper bound of the random number created."
             :required      false
             :default       1000}})


(def create-random-integer-action
  "An example Action that create a random integer."
  {:name            "Random Integer"
   :key             :random-integer
   :description     "Create a random integer and writes it to the context."
   :scopes          [:repository :product]
   :impure?     		false
   :fn              (fn [ctx]
                      {:outputs {:number (rand-int (get-in ctx [:job :inputs :maximum] (get-in random-integer-action-inputs [:maximum :default])))}
                       :failure false})
   :inputs          random-integer-action-inputs
   :outputs         {:number  {:type        :integer
                               :description "The number created by the action"}}})


(def increment-integer-action
  "An example Action that increments an integer by one."
  {:name            "Random Integer"
   :key             :random-integer
   :description     "Create a random integer and writes it to the context."
   :scopes          [:repository :product]
   :impure?     		false
   :fn              (fn [ctx]
                      {:outputs {:number (inc (get-in ctx [:job :inputs :number]))}
                       :failure false})
   :inputs          {:number  {:type          :integer
                               :description   "The integer that gets incremented."
                               :required      true}}
   :outputs         {:number  {:type        :integer
                               :description "The number created by the action."}}})



(def lines-of-code-action
  ""
  {:name            "Lines of code"
   :key             :lines-of-code
   :description     "Count the lines of code using `com.mjdowney.loc`."
   :scopes          [:repository :product]
   :impure?     		false
   :fn              timpl/lines-of-code-action-impl
   :inputs          {:paths   {:type          :vector
                               :description   "Vector with src paths to be analyzed."
                               :required      true}}
   :outputs         {:report  {:type          :string
                               :description   "The report in text form with the lines of code."}}})

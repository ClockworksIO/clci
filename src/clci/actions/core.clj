(ns clci.actions.core
  "This module provides a number of common Actions that are ready to be used
   in any project's workflow."
  (:require
    [clci.actions.impl :as impl]))



(def conventional-commit-linter-action
  "An example Action to run the CC linter on git commit messages."
  {:name            "Lint Commit Message: Conventional Commit"
   :key             :lint-git-commit-msg-with-conventional-commit-spec
   :description     "Takes the commit message and test if it follows the Conventional Commit specification."
   :scopes          [:repository]
   :fn              impl/conventional-commit-linter-action-impl
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
  "An Action to count the lines of code in the specified Clojure files."
  {:name            "Lines of code"
   :key             :lines-of-code
   :description     "Count the lines of code using `com.mjdowney.loc`."
   :scopes          [:repository :product]
   :impure?     		false
   :fn              impl/lines-of-code-action-impl
   :inputs          {:paths   {:type          :vector
                               :description   "Vector with src paths to be analyzed."
                               :required      true}}
   :outputs         {:report  {:type          :string
                               :description   "The report in text form with the lines of code."}}})


(def git-staged-files-action
  "Action to get all files staged for the next commit."
  {:name            "Staged Files"
   :key             :git-staged-files
   :description     "Get all files that were changed and are staged for the next commit."
   :scopes          [:repository]
   :impure?         false
   :fn              impl/git-staged-files-action-impl
   :inputs          {}
   :outputs         {:staged  {:type          :vector
                               :description   "The files staged for the commit."}}})


(def git-add-files-action
  "Action to add a given collection of files to the next commit."
  {:name            "Add Files"
   :key             :git-add-files
   :description     "Add files to the next commit."
   :scopes          [:repository]
   :impure?         true
   :fn              impl/git-add-files-action-impl
   :inputs          {:files   {:type          :vector
                               :description   "The files that are to be added to the next commit."}}
   :outputs         {}})


(def format-clj-action
  "Action to format Clojure source code."
  {:name            "Format Clojure Code"
   :key             :format-clj-code
   :description     "Run the `cljstyle` formatter on all Clojure source files."
   :scopes          [:repository]
   :impure?         true
   :fn              impl/format-clj-action-impl
   :inputs          {:check   {:type          :boolean
                               :description   "Perform a dry-run but don't change the actual source files."}
                     :fix     {:type          :boolean
                               :description   "Run the formatter and automatically fix all style violations."}
                     :no-fail {:type          :boolean
                               :description   "Set to true to in combination with the check option to not stop workflow execution if any style violations are found."}}
   :outputs         {:report  {:type          :string
                               :description   "Report of the formatter run."}}})



(def lint-clj-action
  "Action to run the kondo linter on Clojure source code."
  {:name            "Lint Clojure Code"
   :key             :lint-clj-code
   :description     "Run kondo as linter on all Clojure source files."
   :scopes          [:repository]
   :impure?         false
   :fn              impl/lint-clj-action-impl
   :inputs          {:fail-level  {:type          :keyword
                                   :required      true
                                   :description   "Set the level what issues cause the linter to fail. One of `#{:warning :error :none}`"}
                     :paths       {:type          :vector
                                   :description   "Vector with src paths to be analyzed."
                                   :required      true}}
   :outputs         {:report  {:type          :string
                               :description   "Report of the linter."}}})



(def clj-carve-action
  "Action to run Carve on the Clojure source code."
  {:name            "Carve"
   :key             :lint-clj-code
   :description     "Run carve in check mode on all Clojure source files."
   :scopes          [:repository]
   :impure?         false
   :fn              impl/clj-carve-action-impl
   :inputs          {:no-fail  {:type          :boolean
                                :description   "Set to true if the Action should not stop a workflow if carve finds any issues."}
                     :paths       {:type          :vector
                                   :description   "Vector with src paths to be analyzed."
                                   :required      true}}
   :outputs         {:report  {:type          :string
                               :description   "Report of carve. Sequence of maps describing the problems."}}})


(def antq-action
  "Action to run antq on a CLojure product."
  {:name            "Antq"
   :key             :lint-clj-code
   :description     "Run antq in a Clojure product."
   :scopes          [:repository]
   :impure?         true
   :fn              impl/antq-action-impl
   :inputs          {:check    {:type          :boolean
                                :description   "Set if the Action should check the dependencies without automatically updating them."}
                     :upgrade  {:type          :boolean
                                :description   "Set if the Action should automatically upgrade the dependencies."}
                     :edn      {:type          :boolean
                                :description   "Set to report the found issues in edn format."}}
   :outputs         {:report  {:type          :string
                               :description   "Report of antq."}}})

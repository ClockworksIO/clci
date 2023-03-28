(ns clci.tools.core
  "This modules exposes an api of several tools that can be used
  directly from a bb task in a project's codebase."
  (:require
    [babashka.cli :refer [parse-opts]]
    [clci.actions :refer [lines-of-code-action]]
    [clci.repo :refer [get-paths]]
    [clci.tools.antq :as aq]
    [clci.tools.carve :as carve]
    [clci.tools.cloverage :as cov]
    [clci.tools.format :as fmt]
    [clci.tools.ghooks :as gh]
    [clci.tools.impl :refer [lines-of-code-action-text-reporter lines-of-code-action-edn-reporter]]
    [clci.tools.linter :as linter]
    [clci.tools.mkdocs :as mkdocs]
    [clci.tools.release :as rel]
    [clci.workflow.runner :refer [run-job]]
    [clojure.core.match :refer [match]]
    [clojure.string :as str]))


(defn lint
  "Lint the code."
  [opts]
  (linter/lint opts))


(defn carve!
  "Find and optionally remove unused vars from the codebase."
  [opts]
  (carve/carve! opts))


(defn docs!
  "Build a documentation with mkdocs."
  [opts]
  (mkdocs/docs! opts))


(defn git-hooks
  "Run a git hook."
  [opts]
  (gh/hook opts))


(defn format!
  "Run the formatter on all Clojure files."
  [opts]
  (fmt/format! opts))


(defn- print-help-lines-of-code
  "Print the help of the lines-of-code job."
  []
  (println
    (str/trim
      "
Usage: clci run job lines-of-code <options>

Options:
  --paths       Vector of all paths to be analyzed.
                Defaults to `repo.get-paths`.
  --edn         Return lines of code analysis in edn format.
        
")))


(defn- lines-of-code
  "Get the lines of code of the repo."
  [_]
  (let [spec   {:paths   {:desc         "Paths to consider."
                          :coerce       (fn [s]
                                          (if-let [matches (re-matches #"\[((((.*)\/([^\/\"]*))+),?)+\]" s)]
                                            (-> matches
                                                second
                                                (str/split #","))
                                            ""))
                          :default      (get-paths)}
                :edn     {:desc     "Return lines of code analysis in edn format."
                          :coerce   :boolean
                          :default  false}
                :help    {:desc     "Print help."
                          :coerce   :boolean
                          :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})
        workflow-key :lines-of-code]
    (match [(:help opts) (:edn opts)]
      ;; print help
      [true _]
      (print-help-lines-of-code)
      ;; run the workflow, return report in edn format
      [_ true]
      (run-job
        lines-of-code-action
        "Lines of Code"
        workflow-key
        {:paths (:paths opts)}
        (lines-of-code-action-edn-reporter workflow-key))
      ;; run the workflow, write report in text form to `stdout`
      :else
      (print (run-job
               lines-of-code-action
               "Lines of Code"
               workflow-key
               {:paths (:paths opts)}
               (lines-of-code-action-text-reporter workflow-key))))))


(def lines-of-code-job
  "A job to get the lines of code."
  {:fn          lines-of-code
   :description "Get the lines of code."})


(defn outdated-deps
  "Find outdated dependencies."
  [opts]
  (aq/find-outdated-dependencies opts))


(defn test-coverage
  "Get the test coverage of the repo's code."
  [opts]
  (cov/cloverage opts))


(defn release!
  "Create a new release."
  [opts]
  (rel/release! opts))

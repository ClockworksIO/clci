(ns clci.tools.core
  "This modules exposes an api of several tools that can be used
  directly from a bb task in a project's codebase."
  (:require
    [babashka.cli :refer [parse-opts]]
    [clci.actions.core :refer [lines-of-code-action format-clj-action lint-clj-action]]
    [clci.actions.impl :refer [lines-of-code-action-text-reporter lines-of-code-action-edn-reporter format-clj-action-reporter lint-clj-action-reporter]]
    [clci.repo :refer [get-paths]]
    [clci.term :refer [red]]
    [clci.tools.antq :as aq]
    [clci.tools.carve :as carve]
    [clci.tools.cloverage :as cov]
    [clci.tools.ghooks :as gh]
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


(defn- coerce-paths
  "Coerce a string argument into a vector of paths.
   I.e. `[\"src/\",\"test/\"]`"
  [s]
  (if (vector? s)
    s
    (if-let [matches (re-matches #"\[((((.*)\/([^\/\"]*))+),?)+\]" s)]
      (-> matches
          second
          (str/split #","))
      "")))


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
                          :coerce       coerce-paths
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



(defn- print-help-format-code
  "Print the help of the format-clojure job."
  []
  (println
    (str/trim
      "
Usage: clci run job format <options>

Options:
  --fix         Set as true to format all Clojure source files in place.
                Defaults to false.
  --no-fail     Set as true to not return a non zero exit code on job failure.
        
")))


(defn- format-clojure
  ""
  [_]
  (let [spec   {:fix   {:coerce :boolean
                        :default      false}
                :no-fail {:coerce   :boolean
                          :default  false}
                :help    {:desc     "Print help."
                          :coerce   :boolean
                          :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})
        workflow-key :format-code]
    (cond
      (:help opts)
      (print-help-format-code)
      :else
      (let [report  (run-job
                      format-clj-action
                      "Format Clojure source code"
                      workflow-key
                      {:check (not (get opts :fix))
                       :fix   (get opts :fix false)
                       :no-fail (get opts :no-fail false)}
                      (format-clj-action-reporter workflow-key))]
        (if (and (:failure? report) (not (:no-fail opts)))
          (ex-info "" {})
          (println (:report report)))))))


(defn- print-help-lint-code
  "Print the help of the lint-clojure job."
  []
  (println
    (str/trim
      "
Usage: clci run job lint <options>

Options:
  --fail-level    Set the level what issues cause the linter to fail. 
                  One of `#{:warning :error :none}`. Defaults to :error
  --paths         Vector of all paths to be analyzed by the linter.
                  Defaults to 'src'.
        
")))


(defn- lint-clojure
  ""
  [_]
  (let [spec   {:fail-level   {:coerce    :keyword
                               :default   :error}
                :paths {:coerce   coerce-paths
                        :default  ["src"]}
                :help    {:desc     "Print help."
                          :coerce   :boolean
                          :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})
        workflow-key :lint-code]
    (println "OO" opts)
    (cond
      (:help opts)
      (print-help-lint-code)
      :else
      (let [_ (println "run action as job...")
            report  (run-job
                      lint-clj-action
                      "Lint Clojure source code"
                      workflow-key
                      {:fail-level  (get opts :fail-level)
                       :paths       (get opts :paths)}
                      (lint-clj-action-reporter workflow-key))]
        (if (:failure? report)
          (do
            (println (red "Linter failed"))
            (println (:report report))
            (System/exit 1))
          (println (:report report)))))))


(def format-clojure-job
  "A job to run the Clojure source file formatter."
  {:fn format-clojure
   :description "Format all Clojure source files."})


(def lint-clojure-job
  "A job to run kondo and lint Clojure sources."
  {:fn lint-clojure
   :description "Lint Clojure source files."})


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

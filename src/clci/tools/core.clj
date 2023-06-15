(ns clci.tools.core
  "This modules exposes tools to run on a repo."
  (:require
    [babashka.cli :refer [parse-opts]]
    [clci.actions.core :refer [lines-of-code-action format-clj-action lint-clj-action antq-action update-changelog-action]]
    [clci.actions.impl :refer [lines-of-code-action-text-reporter lines-of-code-action-edn-reporter format-clj-action-reporter lint-clj-action-reporter antq-action-text-reporter antq-action-edn-reporter]]
    [clci.release :as rel]
    [clci.repo :refer [get-paths update-product-version]]
    [clci.term :refer [red blue magenta]]
    [clci.tools.carve :as carve]
    ;; [clci.tools.cloverage :as cov]
    [clci.tools.ghooks :as gh]
    [clci.tools.mkdocs :as mkdocs]
    [clci.workflow.reporter :refer [default-reporter]]
    [clci.workflow.runner :refer [run-job]]
    [clojure.core.match :refer [match]]
    [clojure.string :as str]))


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


(defn- print-help-setup-git-hooks
  "Print the help of the setup git hooks task."
  []
  (println
    (str/trim
      "
Usage: clci setup git-hooks <options>

Options:
  --pre-commit    Set to trigger workflows with the `:git-pre-commit` trigger.
  --commit-msg    Set to trigger workflows with the `:git-commit-msg` trigger.
  --help          Print help.
        
")))


(defn setup-git-hooks
  "Setup git hooks to trigger clci workflows."
  [args]
  (let [pre-commit?   (get-in args [:opts :pre-commit])
        commit-msg?  (get-in args [:opts :commit-msg])]
    (cond
      ;; print help
      (get-in args [:opts :help]) (print-help-setup-git-hooks)
      ;; setup 
      pre-commit? (gh/spit-hook "pre-commit")
      commit-msg? (gh/spit-hook "commit-msg"))))


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
  --help        Print help.
        
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
  --help        Print help.
        
")))


(defn- format-clojure
  "Format Clojure Code."
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
  --help          Print help.
        
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
    (cond
      (:help opts)
      (print-help-lint-code)
      :else
      (let [report  (run-job
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


(defn- print-help-release!
  "Print the help of the release tool."
  []
  (println
    (str/trim
      "
Usage: clci release <options>

Options:
  --update-version    Set if you would like to update the version of all products. 
  --create-releases   Set to create releases for all products.
  --draft             Set if you would like to mark the new release as draft.
  --pre-release       Set if you would like to mark the new release as pre-release.
  --help              Print help.
  
")))


(defn release!
  "Create a new release."
  [_]
  (let [spec   {:update-version   {:coerce :boolean :default false :desc "Set if you would like to update the version of all products."}
                :create-releases  {:coerce :boolean :Default false :desc "Set to create releases for all products."}
                :draft            {:coerce :boolean :desc "Set if you would like to mark the new release as draft."}
                :pre-release      {:coerce :boolean :desc "Set if you would like to mark the new release as pre-release."}
                :help             {:coerce   :boolean
                                   :desc     "Print help."
                                   :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})]
    (cond
      ;; print help
      (:help opts)
      (print-help-release!)
      ;; update product versions
      (:update-version opts)
      (let [new-versions    (rel/derive-current-commit-version)]
        (println (blue "[NEW RELEASES] - Set new versions for products:"))
        (doseq [[key version] new-versions]
          (println (magenta (format "%s for product %s" version key)))
          (update-product-version version key)))
      ;; create new releases
      (:create-releases opts)
      (do
        (println (blue "[NEW RELEASES] - Create Releases"))
        (rel/create-releases))
      ;; fallback to printing the help
      :else
      (print-help-release!))))


(defn- print-help-outdated
  "Print the help of the outdated tool."
  []
  (println
    (str/trim
      "
Usage: clci outdated <options>

Options:
  --check       Set if the Action should check the dependencies without automatically updating them.
  --upgrade     Set if the Action should automatically upgrade the dependencies.
  --edn         Set to report the found issues in edn format.
  --help        Print help.
  
")))


(defn- outdated
  "Check for outdated dependencies ad-hoc job implementation."
  [_]
  (let [spec   {:check    {:coerce :boolean :default true :desc "Set if the Action should check the dependencies without automatically updating them."}
                :upgrade  {:coerce :boolean :default false :desc "Set if the Action should automatically upgrade the dependencies."}
                :edn      {:coerce :boolean :default false :desc "Set to report the found issues in edn format."}
                :help             {:coerce   :boolean
                                   :desc     "Print help."
                                   :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})
        workflow-key :outdated-deps
        reporter (if (:edn opts)
                   (antq-action-edn-reporter workflow-key)
                   (antq-action-text-reporter workflow-key))]
    (cond
      ;; print help
      (:help opts)
      (print-help-outdated)
      ;; run antq
      (or (:check opts) (:upgrade opts))
      (let [report  (run-job
                      antq-action
                      "Find outdated dependencies"
                      workflow-key
                      {:check     (:check opts)
                       :upgrade   (:upgrade opts)
                       :edn       (:edn opts)}
                      reporter)]
        (if (:failure? report)
          (ex-info "" {})
          (println (:report report))))
      ;; fallback to printing the help
      :else
      (print-help-outdated))))


(defn- print-help-update-changelog
  "Print the help of the update-changelog tool."
  []
  (println
    (str/trim
      "
Usage: clci update-changelog <options>

Options:
  --release     Optional release name to use to create a new release section.
  --help        Print help.
  
")))


(defn- update-changelog
  "Update the changelog."
  [_]
  (let [spec   {:release          {:coerce   :string
                                   :desc     "Optional release name used to update the changelog with a release."
                                   :default  nil}
                :help             {:coerce   :boolean
                                   :desc     "Print help."
                                   :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})
        workflow-key :update-changelog
        reporter default-reporter]
    (cond
      ;; print help
      (:help opts)
      (print-help-update-changelog)
      ;; run changelog update
      :else
      (let [report  (run-job
                      update-changelog-action
                      "Update the Changelog"
                      workflow-key
                      {:release     (:release opts)}
                      reporter)]
        (if (:failure? report)
          (ex-info "" {})
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


(def outdated-job
  "A job to find outdated dependencies."
  {:fn          outdated
   :description "Find outdated dependencies."})


(def update-changelog-job
  "A job to update the changelog."
  {:fn          update-changelog
   :description "Update the changelog."})


;; TODO: not working yet
;; (defn test-coverage
;;   "Get the test coverage of the repo's code."
;;   [opts]
;;   (cov/cloverage opts))


(defn carve!
  "Find and optionally remove unused vars from the codebase."
  [opts]
  (carve/carve! opts))


(defn docs!
  "Build a documentation with mkdocs."
  [opts]
  (mkdocs/docs! opts))

(ns clci.tools.core
  "This modules exposes tools to run on a repo."
  (:require
    [babashka.cli :refer [parse-opts]]
    [clci.actions.core :refer [antq-action format-clj-action
                               lines-of-code-action lint-clj-action]]
    [clci.actions.impl :refer [antq-action-edn-reporter
                               antq-action-text-reporter
                               format-clj-action-reporter
                               lines-of-code-action-edn-reporter
                               lines-of-code-action-text-reporter
                               lint-clj-action-reporter]]
    [clci.changelog :refer [update-brick-changelog! update-product-changelog!]]
    [clci.release :as rel]
    [clci.repo :refer [get-brick-by-key get-bricks get-paths get-product-by-key
                       get-products read-repo update-brick-version
                       update-product-version]]
    [clci.repo :as rp]
    [clci.semver :as sv]
    [clci.term :refer [blue green magenta red yellow]]
    [clci.tools.carve :as carve]
    ;; [clci.tools.cloverage :as cov]
    [clci.tools.ghooks :as gh]
    [clci.tools.mkdocs :as mkdocs]
    [clci.workflow.runner :refer [run-job]]
    [clojure.core.match :refer [match]]
    [clojure.string :as str])
  (:import
    java.time.LocalDateTime
    java.time.format.DateTimeFormatter))


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


;; TODO: UPDATE TO ALIGN WITH PRODUCT SCOPE ACTIONS/JOBS!
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


;; TODO: UPDATE TO ALIGN WITH PRODUCT SCOPE ACTIONS/JOBS!
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


;; TODO: UPDATE TO ALIGN WITH PRODUCT SCOPE ACTIONS/JOBS!
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
    --update-version    Sete to update the versions of bricks and products based on the commit history.
    --release           Set to create a new Release using the Github API.
    --pre-release       Set if you would like to mark the new release as pre-release.
    --dry-run           If set, no actual release is created. Prints the information how a release would look like.
    --help              Print help.
  
")))


(defn get-new-version-for-key
  ""
  [versions p-key]
  (->> versions
       (filter
         (fn [[key _]] (= key p-key)))
       first
       second))


(defn- version-updates
  [products new-versions]
  (mapv (fn [{:keys [key version]}]
          {:product key
           :current-version (sv/version-str->vec version)
           :new-version (or (get-new-version-for-key new-versions key) (sv/version-str->vec version))})
        products))


(defn- brick-updates
  [bricks new-versions]
  (mapv (fn [{:keys [key version]}]
          {:brick key
           :current-version (sv/version-str->vec version)
           :new-version (or (get-new-version-for-key new-versions key) (sv/version-str->vec version))})
        bricks))


(defn update-product-versions!
  ""
  [product-version-updates]
  (doseq [update product-version-updates]
    (println (str (magenta (:current-version update)) (yellow "->")  (magenta (:new-version update)) "for product") (magenta (:product update)))
    (update-product-version (sv/version-vec->str (:new-version update)) (:product update))))


(defn update-brick-versions!
  ""
  [brick-version-updates]
  (doseq [update brick-version-updates]
    (println (str (magenta (:current-version update)) (yellow "->")  (magenta (:new-version update)) "for product") (magenta (:brick update)))
    (update-brick-version (sv/version-vec->str (:new-version update)) (:brick update))))


(defn release!-impl
  ""
  [update-version? release? pre-release? dry-run?]
  (let [repo            (read-repo)
        new-product-versions  (rel/calculate-products-version repo)
        product-updates       (version-updates (get-products repo) new-product-versions)
        new-brick-versions    (rel/calculate-bricks-version repo)
        brick-updates         (brick-updates (get-bricks repo) new-brick-versions)]
    (println (blue "[1 / 3] - Update bricks version information."))
    (if (and update-version? (not dry-run?))
      (update-brick-versions! brick-updates)
      (println "skipping"))
    (println (blue "[2 / 3] - Update product version information."))
    (if (and update-version? (not dry-run?))
      (update-product-versions! product-updates)
      (println "skipping"))
    (println (blue "[3 / 3] - Create Releases on Github."))
    (if (and release? (not dry-run?))
      (rel/release-new-products! pre-release?)
      (println "skipping"))
    (println (green "successful"))))


(defn release!
  "Create a new release."
  [_]
  (let [spec   {:release          {:coerce :boolean :desc "Set if you would like create a new Release."}
                :update-version   {:coerce :boolean :desc "Set if you would like update the versions of bricks and products."}
                :pre-release      {:coerce :boolean :desc "Set if you would like to mark the new release as pre-release."}
                :dry-run          {:coerce :boolean :desc "If set, no actual release is created. Prints the information how a release would look like."}
                :help             {:coerce   :boolean
                                   :desc     "Print help."
                                   :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})]
    (println (blue "[RELEASE] - Create new Product Releases"))
    (when (:dry-run opts)
      (println (yellow "-- Dry Run --")))

    (cond
      ;; print help
      (:help opts)
      (print-help-release!)
      ;; Create the releases
      :else
      (release!-impl (:update-version opts) (:release opts) (:pre-release opts) (:dry-run opts)))))


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


;; TODO: UPDATE TO ALIGN WITH PRODUCT SCOPE ACTIONS/JOBS!
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


(defn- print-help-changelog!
  "Print the help of the changelog tool."
  []
  (println
    (str/trim
      "
Usage: clci changelog <options>

Options:
  --product     The key of the product to generate the changelog for.
                Use `:ALL` to update all product changelogs.
  --brick       The key of the brick to generate the changelog for.
                Use `:ALL` to update all brick changelogs.
  --version     Optional (release) version used to update the changelog. 
                When not set, changes are put in the 'Unreleased' section.
  --help        Print help.
  
")))


(defn update-changelog!-impl
  "Implements the changelog update.
   Takes the `brick` and `product` keywords to specify which brick or
   product changelog should be updated. When `:ALL` is provided as
   value, then all brick or product changelogs are updated."
  [brick product version]
  (let [repo        (rp/read-repo)
        products    (rp/get-products repo)
        bricks      (rp/get-bricks repo)
        date-today  (fn []
                      (.format (LocalDateTime/now) (DateTimeFormatter/ofPattern "yyyy-MM-dd")))]
    (println (blue "Updating the Changelogs"))
    (println (blue "[1 / 2] Updating Brick Changelogs"))
    ;; first the bricks
    (cond
      ;; update all brick changelogs
      (= :ALL brick)
      (do
        (println (blue ""))
        (doseq [brick bricks]
          (update-brick-changelog! repo brick)))
      ;; update the changelog of a specific brick
      (some? brick)
      (update-brick-changelog! repo (get-brick-by-key brick repo) {:version version :published (date-today)})
      :else
      (println (yellow "No product selected - skipping.")))
    ;; then the products
    (println (blue "[2 / 2] Updating Product Changelogs"))
    (cond
      ;; update all product changelogs
      (= :ALL product)
      (do
        (println (blue ""))
        (doseq [product products]
          (update-product-changelog! repo product)))
      ;; update the changelog of a specific product
      (some? product)
      (update-product-changelog! repo (get-product-by-key product repo) {:version version :published (date-today)})
      :else
      (println (yellow "No product selected - skipping.")))
    (println (green "Successfully updated the changelogs."))))


(defn changelog!
  "Update the changelog."
  [_]
  (let [spec   {:product          {:coerce :keyword :desc "The key of the product to generate the changelog for."}
                :brick            {:coerce :keyword :desc "The key of the brick to generate the changelog for."}
                :version          {:coerce   :string
                                   :desc     "Optional (release) version used to update the changelog. When not set, changes are put in the 'Unreleased' section."
                                   :default  nil}
                :help             {:coerce   :boolean
                                   :desc     "Print help."
                                   :default  false}}
        opts   (parse-opts *command-line-args* {:spec spec})]
    (cond
      ;; print help
      (:help opts)
      (print-help-changelog!)
      ;; run changelog update
      :else
      (update-changelog!-impl (:brick opts) (:product opts) (:version opts)))))


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


;; (def update-changelog-job
;;   "A job to update the changelog."
;;   {:fn          update-changelog
;;    :description "Update the changelog."})


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

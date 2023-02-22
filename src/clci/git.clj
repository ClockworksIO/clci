(ns clci.git
  "Various git tooling."
  (:require
    [babashka.process :refer [sh shell]]
    [clojure.string :as str]))


(defn get-tags
  "Get a list of all tags."
  []
  (-> (shell {:out :string} "git tag")
      :out
      str/split-lines))


(defn current-commit
  "Get the current commit on the current branch."
  []
  (-> (sh "git" "rev-parse" "HEAD")
      :out
      str/trim))


(defn commits-on-branch-since
  "Get all commits using git cli.
  Reads the git log using the oneline format. Optionally takes
  | key          | description |
  | -------------|-------------|
  | `:branch`    | (optional) The branch to get the commits. Defaults to `master`
  | `:since`     | (optional) Commit SHA since when to get all commits. Defaults to the first commit on the branch.
  | `:with-tags` | (optional) When set, the log will include commits that add a tag. Defaults to `false`.
  "
  ([] (commits-on-branch-since {}))
  ([{:keys [branch since with-tags]
     :or {branch    "master"
          since     nil
          with-tags false}}]
   (let [;; sanitize a single log entry into a map
         ;; used after grouping the log lines into commits
         commit-col->map   (fn [v]
                             {:hash    (get v 0)
                              :date    (get v 1)
                              :author  (get v 2)
                              :subject (get v 3)
                              :files   (subvec v 4)})
         ;; git shell command used to get the log in the format required
         cmd (str/join
               " "
               ["git log --format=\"%n%n%n%H%n%ai%n%ae%n%s\""
                "--name-only"
                (when-not with-tags "--decorate-refs-exclude=refs/tags")
                (format "--first-parent %s" branch)
                (when since (format "%s..HEAD" since))])]
     (as-> (shell {:out :string} cmd) $
           (:out $)
           (str/split $ #"\n\n\n")
           (remove str/blank? $)
           (map
             (comp commit-col->map (partial into []) #(remove str/blank? %) str/split-lines)
             $)))))



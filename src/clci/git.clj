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


(defn current-branch-name
  "Get the name of the current branch."
  []
  (->
    (shell {:out :string} "git rev-parse --abbrev-ref HEAD")
    :out))


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
                              :body    (get v 4)
                              :files   (get v 5)})
         ;; split entries on linebreak and sanitize whitespace
         splitter (comp (partial into []) #(remove str/blank? %) str/split-lines)
         ;; git shell command used to get the log in the format required
         cmd (str/join
               " "
               ["git log --format=\"%n!-M-!%H%n%ai%n%ae!-S-!%s!-B-!%b!-F-!\""
                "--name-only"
                (when-not with-tags "--decorate-refs-exclude=refs/tags")
                (format "--first-parent %s" branch)
                (when since (format "%s..HEAD" since))])]
     (as-> (shell {:out :string} cmd) $
           (:out $)
           (str/split $ #"!-M-!")
           (remove str/blank? $)
           (map #(str/split % #"(!-S-!)|(!-B-!)|(!-F-!)") $)
           (map (fn [[head subject body changes]]
                  (commit-col->map (apply conj [(splitter head) subject body (splitter changes)])))
                $)))))


(defn staged-files
  "Get a collection of all changed files staged for the next commit."
  []
  (-> (shell {:out :string} "git --no-pager diff --name-only --no-color --cached --diff-filter=ACM")
      :out
      str/split-lines))

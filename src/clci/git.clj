(ns clci.git
  "Various git tooling."
  (:require
   [babashka.process :refer [sh shell]]
   [clci.conventional-commit :as cc]
   [clci.semver :as sv]
   [clojure.string :as str]))

(defn get-release-tags
  "Get a list of all release tags.
  A release tag must follow the semantic versioning.
  examples:
  - `1.0.3`
  - `1.2.0-pre-20230103`
  - `1.2.3-ac3619f0d7`"
  []
  (as-> (shell {:out :string} "git tag") $
    (:out $)
    (str/split-lines $)
    (filter (fn [tag] (re-matches sv/semver-re tag)) $)))

;; (defn latest-release
;;   "Get the latest release.
;;   The latest release is the one with the highest version number."
;;   [releases]
;;   (-> (v/version-sort releases)
;;       last))

(defn get-latest-release
  "Fake Adapter to get the latest release.
  TODO: Use the proper GH API for this instead!"
  []
  {:commit  "4d38687e63c8de636c5aa2b475e6c337b9b9f4f1"
   :tag     "0.3.1"
   :name    "Version 0.3.1"})

(defn derive-release-version
  "Get the version of the given `release` in a convenient vec format.
  Returns a vector in the format `[major minor patch pre-release]` where
  the first three parts are integers and the pre-release part is a string."
  [release]
  [(sv/major (:tag release))
   (sv/minor (:tag release))
   (sv/patch (:tag release))
   (sv/pre-release (:tag release))])

(defn latest-commit
  "Get the latest commit on the current branch."
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
  | `:since`     | (optional) Commit ID since when to get all commits. Defaults to the first commit on the branch.
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

(defn- dervice-version-from-commits
  "Derive a version based on an inital version and a list of commits.
  Takes the `inital-version` as a 4-tuple vector and a `commits` list with pairs
  `[type breaking?]`. The _type_ denotes the change type as a string and the 
  _breaking?_ boolean flag indicates if the change is marked as breaking. 
  The commits are ordered newest-commit...oldest-commit."
  [inital-version commits]
  (let [inc-major   (fn [[ma _ _ _]] [(inc ma) 0 0 nil])
        inc-minor   (fn [[ma mi _ _]] [ma (inc mi) 0 nil])
        inc-patch   (fn [[ma mi p _]] [ma mi (inc p) nil])
        inc-version (fn [[t b?] v]
                      (cond
                        b?            (inc-major v)
                        (= t "feat")  (inc-minor v)
                        (= t "fix")   (inc-patch v)
                        :else         v))]
    (reduce
     (fn [acc commit] (inc-version commit acc))
     inital-version
     (reverse commits))))

(defn derive-current-commit-version
  "Derive the version of the current codebase.
  Uses the latest release that exists and the git log which must follow the conventional commits
  specification. Depending on the type of the commit the new version will be calculated
  following the semantic versioning specs."
  []
  (let [;; get the last release using the gh api
        last-release          (get-latest-release)
        ;; we need the version as an easy to manipulate datastructure, not just as string
        last-release-version  (derive-release-version last-release)
        ;; get a git log of all commits since the latest release
        commit-log            (commits-on-branch-since {:since (:commit last-release)})
        ;; parse the git log entries, discard those not following the conventional commit specs
        semver-entries        (cc/parse-only-valid (map :subject commit-log))
        ;; extract the type of the commits and if they have a breaking change
        commit-types          (map (fn [e] [(cc/get-type e) (cc/is-breaking? e)]) semver-entries)]
    ;; using all information we collected we can now calculate the new version
    (dervice-version-from-commits last-release-version commit-types)))

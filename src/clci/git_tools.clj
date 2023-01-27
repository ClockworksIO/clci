(ns clci.git-tools
  "Various git tooling except hooks."
  (:require
    ;; [babashka.fs :as fs]
    [babashka.process :refer [sh shell]]
    [clci.semver :refer [semver-re]]
    [clojure.string :as str]
    [version-clj.core :as v]))


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
        (filter (fn [tag] (re-matches semver-re tag)) $)))


(defn latest-release
  "Get the latest release.
  The latest release is the one with the highest version number."
  [releases]
  (-> (v/version-sort releases)
      last))


(defn latest-commit
  "Get the latest commit on the current branch."
  []
  (-> (sh "git" "rev-parse" "HEAD")
      :out
      str/trim))

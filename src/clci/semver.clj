(ns clci.semver
  "Semantic Versioning related functionality."
  (:require
    [clci.conventional-commit :as cc]
    [clci.gh.core :as gh]
    [clci.git :as git]
    [clci.util :refer [read-repo]]
    [clojure.string :as str]))


(def semver-re
  "Regular Expression to match version strings following the
  Semantic Versioning specification.
  See https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string."
  #"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")


(defn valid-version-tag?
  "Predicate to test if the given `tag` contains a string that follows
  the semantic versioning convention."
  [tag]
  (some? (re-matches semver-re tag)))


(defn major
  "Get the major part of the version."
  [version]
  (when (re-matches semver-re version)
    (-> (str/split version #"\.|-")
        (first)
        (Integer/parseInt))))


(defn minor
  "Get the minor part of the version."
  [version]
  (when (re-matches semver-re version)
    (-> (str/split version #"\.|-")
        (second)
        (Integer/parseInt))))


(defn patch
  "Get the patch part of the version."
  [version]
  (when (re-matches semver-re version)
    (-> (str/split version #"\.|-")
        (nth 2)
        (Integer/parseInt))))


(defn pre-release
  "Get the pre-release part of the version - if any."
  [version]
  (when (re-matches semver-re version)
    (-> (str/split version #"-" 2)
        (get 1 nil))))


(defn- get-latest-release
  "Adapter to get the latest release.
  **Note**: Only supports the Github API to get the latest release at this time."
  []
  (let [repo-conf   (read-repo)
        repo        (get-in repo-conf [:scm :github :repo])
        owner       (get-in repo-conf [:scm :github :owner])
        release     (gh/get-latest-release owner repo)
        tag         (gh/get-tag owner repo (:tag_name release))]
    {:commit  (get-in tag [:object :sha])
     :tag     (:tag_name release)
     :name    (:name release)}))


(defn- get-release-tags
  "Get a list of all release tags.
  A release tag must follow the semantic versioning.
  examples:
  - `1.0.3`
  - `1.2.0-pre-20230103`
  - `1.2.3-ac3619f0d7`"
  []
  (filter (fn [tag] (re-matches semver-re tag)) (git/get-tags)))


(defn- derive-release-version
  "Get the version of the given `release` in a convenient vec format.
  Returns a vector in the format `[major minor patch pre-release]` where
  the first three parts are integers and the pre-release part is a string.
  A `release` must be a map with the keys :commit, :tag and :name."
  [release]
  [(major (:tag release))
   (minor (:tag release))
   (patch (:tag release))
   (pre-release (:tag release))])


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
        commit-log            (git/commits-on-branch-since {:since (:commit last-release)})
        ;; parse the git log entries, discard those not following the conventional commit specs
        semver-entries        (cc/parse-only-valid (map :subject commit-log))
        ;; extract the type of the commits and if they have a breaking change
        commit-types          (map (fn [e] [(cc/get-type e) (cc/is-breaking? e)]) semver-entries)]
    ;; using all information we collected we can now calculate the new version
    (dervice-version-from-commits last-release-version commit-types)))

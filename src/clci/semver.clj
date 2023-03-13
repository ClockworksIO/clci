(ns clci.semver
  "Semantic Versioning related functionality."
  (:require
    ;; [clci.conventional-commit :as cc]
    ;; [clci.gh.core :as gh]
    ;; [clci.git :as git]
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


(defn valid-semver-str?
  "Predicate to test if the given string `s` follows the SemVer specification."
  [s]
  (some? (re-matches semver-re s)))


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


(defn version-str->vec
  "Get the version of the given `release` in a convenient vec format.
  Returns a vector in the format `[major minor patch pre-release]` where
  the first three parts are integers and the pre-release part is a string.
  A `release` must be a map with the keys :commit, :tag and :name."
  [version-str]
  [(major version-str)
   (minor version-str)
   (patch version-str)
   (pre-release version-str)])


(defn version-vec->str
  "Takes a vector `version` in the form [major minor patch pre-release] and
  returns a string of the version following the SemVer spec."
  [version]
  (str/join "." (remove nil? version)))


(defn newer?
  "Compare two release versions `v1` and `v2` and return true if v1 is newer than v2."
  [v1 v2]
  (let [[v1-maj v1-min v1-patch _] (version-str->vec v1)
        [v2-maj v2-min v2-patch _] (version-str->vec v2)]
    (cond
      (> (int v1-maj) (int v2-maj)) true
      (> (int v1-min) (int v2-min)) true
      (> (int v1-patch) (int v2-patch)) true
      :else false)))

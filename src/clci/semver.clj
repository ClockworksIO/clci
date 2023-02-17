(ns clci.semver
  "Semantic Versioning related functionality."
  (:require
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

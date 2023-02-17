(ns user
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [shell]]
    [clci.term :refer [red]]
    [clojure.edn :as edn]
    [clojure.string :as str]))


(defn join-paths
  "Takes an arboitrary number of (partital) paths and joins them together.
  Handles slashes at the end of the path.
  I.e. 
  ```clojure
  (join-paths \"/some/base/path\" \"local/path/\") ; -> \"/some/base/path/local/path\"
  (join-paths \"/some/base/path/\" \"local/path\") ; -> \"/some/base/path/local/path\"
  ```
  "
  [& parts]
  (as-> (map #(str/split % #"/") parts) $
        (apply concat $)
        (str/join "/" $)))


(def repo-config
  "Configuration of the repository."
  (atom nil))


(defn- read-edn-file
  "Read an edn file from the given `path`."
  [path]
  (when (fs/exists? path)
    (-> (slurp path)
        (edn/read-string))))


(defn- read-deps-edn
  "Read the deps.edn file from a project.
	Takes the the path of the `project-root` and an optional collection of `filter-keys`
	representing the keys that will be read from the deps.edn file."
  [project-root & {:keys [filter-keys] :or {filter-keys [:paths :deps]}}]
  (-> (read-edn-file (join-paths project-root "deps.edn"))
      (select-keys filter-keys)))


;; (read-deps-edn "./fintools")

(defn- read-bb-edn
  "Read the bb.edn file from a project.
	Takes the the path of the `project-root`."
  [project-root & {:keys [filter-keys] :or {filter-keys [:paths :deps]}}]
  (-> (read-edn-file (join-paths project-root "bb.edn"))
      (select-keys filter-keys)))


;; (read-bb-edn "./fintools")

(defn- read-repo-config
  "Read the repo config.
	Reads the repository configuration either from the default path
  or a given `path`."
  ([] (read-repo-config "repo.edn"))
  ([path]
   (as-> (slurp path) $
         (edn/read-string $)
         (assoc $ :projects (mapv
                              (fn [p]
                                (assoc p
                                       :deps-edn (read-deps-edn (:root p))
                                       :bb-edn (read-bb-edn (:root p))))
                              (:projects $)))
         (reset! repo-config $))))


(read-repo-config "repo.edn")

@repo-config



(def collect-all-paths
  ""
  [])



;; (commits-on-branch-since {:since "4d38687e63c8de636c5aa2b475e6c337b9b9f4f1"})

(defn get-latest-release
  "Fake Adapter to get the latest release.
  TODO: Use the proper GH API for this instead!"
  []
  {:commit  "4d38687e63c8de636c5aa2b475e6c337b9b9f4f1"
   :tag     "0.3.1"
   :name    "Version 0.3.1"})


(str/split "1.2.3-ff-3-33" #"-" 2)


(Integer/parseInt "2")


(def semver-re
  "Regular Expression to match version strings following the
  Semantic Versioning specification.
  See https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string."
  #"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")


(defn pre-release
  "Get the pre-release part of the version - if any."
  [version]
  (when (re-matches semver-re version)
    (-> (str/split version #"-" 2)
        (get 1 nil))))


(pre-release "2.2.77")


(def tree-1
  '([:TYPE "feat"]
    [:SCOPE "mobile"]
    [:SUBJECT
     [:TEXT "switch to new API "]
     [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
    [:BODY
     [:PARAGRAPH
      [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]
     [:PARAGRAPH
      [:TEXT "One more paragraph."]]]
    [:FOOTER
     [:FOOTER-ELEMENT
      [:FOOTER-TOKEN "BREAKING CHANGE"]
      [:FOOTER-VALUE [:TEXT "will not work with library xzy before 0.2.4"]]]
     [:FOOTER-ELEMENT
      [:FOOTER-TOKEN "note"]
      [:FOOTER-VALUE [:TEXT "Thanks for all the fish."]]]]
    [:GIT-REPORT
     [:COMMENT " Please enter the commit message for your changes. Lines starting"]
     [:COMMENT "  with '#' will be ignored, and an empty message aborts the commit."]
     [:COMMENT ""]
     [:COMMENT " Date:      Thu Dec 8 17:01:23 2022 +0100"]
     [:COMMENT " On branch feat/rr-2"]
     [:COMMENT " Changes to be committed:"]
     [:COMMENT "       renamed:    ci-bb/src/clj/ci_bb/pod.clj -> ci-bb/src/clj/ci_bb/main.clj"]]))


(defn subtree-by-token
  "Implements a depth-first search on the abstract syntax `tree` to find
  the first subtree identified by the given `token`."
  [tree token]
  (let [token'        (first (first tree))
        child         (rest (first tree))
        tail          (rest tree)]
    (cond
      (= token' token)        child
      (coll? (first child))   (if-let [rec-res (subtree-by-token child token)]
                                rec-res
                                (subtree-by-token tail token))
      (empty? tail)           nil
      :else                   (subtree-by-token tail token))))


(subtree-by-token tree-1 :TYPE)

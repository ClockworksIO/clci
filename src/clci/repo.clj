(ns clci.repo
  "This module provides methods to read and update the repo.edn file."
  (:require
    [clci.semver :as sv]
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as s]))


;;
;; Generic Utilities to work with project (edn) files ;;
;;

(defn pretty-spit!
  "Write a clojure datastructure in an edn text file.
  Takes the `path` of the file and ther `data`. 
  **warning**: Will override the file's contents!"
  [path data]
  (binding [*print-namespace-maps* false]
    (pprint data (clojure.java.io/writer path))))


(defn- slurp-edn
  "Read an edn file and parse the content as clojure datastructure."
  [path]
  (-> (slurp path)
      (edn/read-string)))


;;
;; Utilities to work with deps.edn ;;
;;


(defn read-deps
  "Read the deps.edn file."
  []
  (-> (slurp-edn "deps.edn")))


(defn write-deps!
  "Write the `data` to the deps.edn file.
   **warning**: Will override the file's contents!"
  [data]
  (pretty-spit! "deps.edn" data))



;;
;; Utilities to work with bb.edn ;;
;;

(defn read-bb
  "Read the bb.edn file."
  []
  (-> (slurp-edn "bb.edn")
      ;; TODO: Add checks that the mandatory fields are here!
      ))


(defn write-bb!
  "Write the `data` to the bb.edn file.
   **warning**: Will override the file's contents!"
  [data]
  (pretty-spit! "bb.edn" data))


(defn add-task
  "Add a new task to the bb.edn file.
  Takes the `name` which must be a string than can be cast to a symbol
  and identifies the babashka task and the function `t-fn` that is executed
  from the task. Also takes an optional `description` string for 
  the task and an optional list of requirements fo the task.
  
  The function that is executed by the task will be wrapped in a babashka
  (exec t-fn) statement and therefore must be fully qualified. It also should
  take an opts argument and implement the babashka cli API to parse optional
  arguments."
  [{:keys [name t-fn desc reqs]}]
  (-> (read-bb)
      (assoc-in
        [:tasks (symbol name)]
        (cond-> {:task (list 'exec t-fn)}
          (some? desc) (assoc :desc desc)
          (some? reqs) (assoc :requires reqs)))
      (write-bb!)))


;;
;; Utilities to work with repo.edn ;;
;;

(defn read-repo
  "Read the repo configuration."
  []
  (-> (slurp-edn "repo.edn")
      ;; TODO: Add checks that the mandatory fields are here!
      ))


(defn write-repo!
  "Write the `data` to the repo.edn file.
   **warning**: Will override the file's contents!"
  [data]
  (pretty-spit! "repo.edn" data))


(def valid-scm
  "A collection of valid scms."
  #{:git})


(def valid-scm-provider
  "A collection of valid scm provider."
  #{:github})


(defn- scm-url
  "Derive the scm url based on the scm `provider` and the `name` and `owner` of the repository."
  [provider name owner]
  (case provider
    :github (format "git@github.com:%s/%s.git" owner name)
    nil))


(defn- scm-provider-conf
  "Derive the scm provider configuration.
  Takes the scm `provider`, the `name` and `owner` of the repository."
  [provider name owner]
  (case provider
    :github {:name :github :repo name :owner owner}
    nil))


(defn- with-scm-provider
  "Add the scm provider to a base.
  Takes the following arguments:
  | key                 | description                         |
  | --------------------|-------------------------------------|
  | `base`              | (required!) A map representing the repo base.
  | `provider`          | (required!) The provider of the scm service, keyword. See the `valid-scm-provider` collection.
  | `repo-name`         | (required!) Name of the repository, string.
  | `repo-owner`        | (required!) Name of the repository owner, string."
  [base provider repo-name repo-owner]
  (assoc base :provider (scm-provider-conf provider repo-name repo-owner)))


(defn with-single-project
  "Add the basic project configuration for a single project at the
  repositories root to the base.
  Takes the repo `base`, map."
  [base & {:keys [initial-version] :or {initial-version "0.0.0-semver"}}]
  (when-not (sv/valid-version-tag? initial-version)
    (throw
      (ex-info
        "The initial version must follow the semantic versioning specification."
        {:reason :invalid-initial-version})))
  (assoc base
         :projects
         [(cond-> {:root ""}
            (some? initial-version) (assoc :version initial-version))]))


(defn repo-base
  "Create a base for the repo."
  [opts]
  {:scm       (-> {:type    (:scm opts),
                   :url     (scm-url (:scm-provider opts) (:scm-repo-name opts) (:scm-repo-owner opts))}
                  (with-scm-provider (:scm-provider opts) (:scm-repo-name opts) (:scm-repo-owner opts)))})



;; (s/def ::initial-version string?)
(s/def ::scm some?)
(s/def ::scm-provider some?)
(s/def ::scm-repo-name string?)
(s/def ::scm-repo-owner string?)


;; (s/fdef repo-base
;;   :args (s/cat :opts (s/keys :req-un [::scm :scm-provider ::scm-repo-name ::scm-repo-owner]))
;;   :ret map?
;; )

(defn update-version
  "Update the version in the repo.edn file.
  Takes the `version` as string."
  [version]
  (as-> (read-repo) $
        (assoc-in $ [:projects 0 :version] version)
        (pretty-spit! "repo.edn" $)))



;;
;; MONOREPO FUNCTIONALITY ;;
;;

(defn get-paths
  "TODO: combine with utils from monorepo!"
  []
  ["src"])

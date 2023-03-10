(ns clci.tools.release
  "This module provides tools to create releases."
  (:require
    [babashka.cli :as cli]
    [babashka.process :refer [shell]]
    [clci.conventional-commit :as cc]
    [clci.gh.core :as gh]
    [clci.git :as git]
    [clci.repo :as r]
    [clci.semver :as sv]
    [clci.term :as c]
    [clojure.string :as str]))


(defn- get-latest-release
  "Adapter to get the latest release.
  **Note**: Only supports the Github API to get the latest release at this time."
  []
  (let [repo-conf   (r/read-repo)
        repo        (get-in repo-conf [:scm :provider :repo])
        owner       (get-in repo-conf [:scm :provider :owner])
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
  (filter (fn [tag] (re-matches sv/semver-re tag)) (git/get-tags)))


(defn- derive-release-version
  "Get the version of the given `release` in a convenient vec format.
  Returns a vector in the format `[major minor patch pre-release]` where
  the first three parts are integers and the pre-release part is a string.
  A `release` must be a map with the keys :commit, :tag and :name."
  [release]
  [(sv/major (:tag release))
   (sv/minor (:tag release))
   (sv/patch (:tag release))
   (sv/pre-release (:tag release))])


(defn- vec->str
  "Takes a vector `version` in the form [major minor patch pre-release] and
  returns a string of the version following the SemVer spec."
  [version]
  (str/join "." (remove nil? version)))


(defn- derive-version-from-commits
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
    (derive-version-from-commits last-release-version commit-types)))


(def cli-options
  ""
  {:spec
   {:only-update-version 	{:coerce :boolean :desc "Set to true if you would like to update the version."}
    :only-release 				{:coerce :boolean :desc "Set to true if you would like to mark the new release as draft."}
    :draft  			{:coerce :boolean :desc "Set to true if you would like to mark the new release as draft."}
    :pre-release  {:coerce :boolean :desc "Set to true if you would like to mark the new release as pre-release."}
    :report   		{:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :silent       {:coerce :boolean :desc "Set to true if you would like to not write anything to stdout when running i.e. in a CI environment."}
    :gh-action    {:coerce :boolean :desc "Set to true if this is run as part of a GH action. Will set the action output 'version' to the new version."}
    :help  				{:coerce :boolean :desc "Show help."}}})


(defn- print-help
  "Print help for the task."
  []
  (println "Create a new release.\n")
  (println (cli/format-opts cli-options)))


(defmulti release-impl (fn [& args] (first args)))


;; Implementation of release - derive the version of the current Head
;; based on the commit history and Conventional Commits and write the new
;; version (following the Semantic Versioning Spec) to the `repo.edn` file
(defmethod release-impl :only-update-version [_ opts]
  (let [silent?         (:silent opts)
        write-report?   (:report opts)
        gh-action?      (:gh-action opts)
        new-version 		(derive-current-commit-version)
        new-version-str	(vec->str new-version)]
    (when-not silent?
      (println (c/blue "[NEW RELEASE] Set new version"))
      (println "new version:" (c/magenta new-version-str)))
    (r/update-version new-version-str)
    (when gh-action?
      (println (format "echo 'version=%s' >> $GITHUB_OUTPUT" new-version-str))
      (println (shell {:out :string} (format "echo 'version=%s' >> $GITHUB_OUTPUT" new-version-str))))
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))


;; Implementation of release - create a new Release for the current commit
;; on Head. Uses the GH API to create the commit
;; **Important**: Expects that the version for this release was set earlier
;;   in the `repo.edn` config. Please set the version one step before executing
;;   this release step using the _--only-update-version_ option!
(defmethod release-impl :only-release [_ opts]
  (let [pre-release?    (:pre-release opts)
        draft?  				(:draft opts)
        silent?         (:silent opts)
        write-report?   (:report opts)
        repo-conf   		(r/read-repo)
        repo        		(get-in repo-conf [:scm :provider :repo])
        owner       		(get-in repo-conf [:scm :provider :owner])
        version-str     (get-in repo-conf [:projects 0 :version])]
    (when-not silent?
      (println (c/blue "[NEW RELEASE] Create Release")))
    (gh/create-release {:owner owner :repo repo :tag version-str :draft draft? :pre-release pre-release?})
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))


;; (defn- release-impl
;;   "Implementation of release."
;;   [opts]
;;   (let [pre-release?    (:pre-release opts)
;;         draft?  				(:draft opts)
;;         silent?         (:silent opts)
;;         write-report?   (:report opts)
;;         repo-conf   		(r/read-repo)
;;         repo        		(get-in repo-conf [:scm :github :repo])
;;         owner       		(get-in repo-conf [:scm :github :owner])
;;         new-version 		(sv/derive-current-commit-version)
;;         new-version-str	(sv/vec->str new-version)]
;;     (when-not silent?
;;       (println (c/blue "[NEW RELEASE]") (c/magenta new-version-str))
;;       (println "new version:" (c/magenta new-version-str)))
;;     (r/update-version new-version-str)
;;     (gh/create-release {:owner owner :repo repo :tag new-version-str :draft draft? :pre-release pre-release?})
;;     (when write-report?
;;       (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))


(defn release!
  "Create a new release."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:help opts) (print-help)
    (:only-update-version opts) (release-impl :only-update-version opts)
    (:only-release opts) (release-impl :only-release opts)
    :else (print-help)))

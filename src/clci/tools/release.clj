(ns clci.tools.release
  ""
  (:require
    [babashka.cli :as cli]
    [clci.gh.core :as gh]
    [clci.repo :as r]
    [clci.semver :as sv]
    [clci.term :as c]))


(def cli-options
  ""
  {:spec
   {:only-update-version 	{:coerce :boolean :desc "Set to true if you would like to update the version."}
    :only-release 				{:coerce :boolean :desc "Set to true if you would like to mark the new release as draft."}
    :draft  			{:coerce :boolean :desc "Set to true if you would like to mark the new release as draft."}
    :pre-release  {:coerce :boolean :desc "Set to true if you would like to mark the new release as pre-release."}
    :report   		{:coerce :boolean :desc "Set to true if you would like to write the result to a report file."}
    :silent       {:coerce :boolean :desc "Set to true if you would like to not write anything to stdout when running i.e. in a CI environment."}
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
        new-version 		(sv/derive-current-commit-version)
        new-version-str	(sv/vec->str new-version)]
    (when-not silent?
      (println (c/blue "[NEW RELEASE] Set new version"))
      (println "new version:" (c/magenta new-version-str)))
    (r/update-version new-version-str)
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
        repo        		(get-in repo-conf [:scm :github :repo])
        owner       		(get-in repo-conf [:scm :github :owner])
        version-str     (get-in repo-conf [:version])]
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

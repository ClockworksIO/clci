(ns clci.tools.release
  "This module provides tools to create releases."
  (:require
    [babashka.cli :as cli]
    [clci.release :as rel]
    ;; [clci.gh.core :as gh]
    [clci.repo :as r]
    [clci.term :as c]
    [clojure.pprint :refer []]))



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


;; Implementation of release - for a repo with multiple projects - derive 
;; the version of the current Head based on the commit history and 
;; Conventional Commits and write the new versions (following the 
;; Semantic Versioning Spec) to the `repo.edn` file
(defmethod release-impl :only-update-version [_ opts]
  (let [silent?         (:silent opts)
        write-report?   (:report opts)
        gh-action?      (:gh-action opts)
        new-versions    (rel/derive-current-commit-version)]
    (when-not silent?
      (println (c/blue "[NEW RELEASE] Set new version"))
      (println "new versions:")
      (doseq [[key version] new-versions]
        (println (c/magenta (format "%s for product %s" version key)))))
    (doseq [[key version] new-versions]
      (r/update-version version key))
    (when gh-action?
      (spit (System/getenv "GITHUB_OUTPUT") (format "versions=%s\n" new-versions) :append true))
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))


;; Implementation of release - create a new Release for the current commit
;; on Head. Uses the GH API to create the commit
;; **Important**: Expects that the version for this release was set earlier
;;   in the `repo.edn` config. Please set the version one step before executing
;;   this release step using the _--only-update-version_ option!
(defmethod release-impl :only-release [_ opts]
  (let [pre-release?    (:pre-release opts false)
        draft?  				(:draft opts false)
        silent?         (:silent opts false)
        write-report?   (:report opts false)]
    (when-not silent?
      (println (c/blue "[NEW RELEASE] Create Release")))
    (rel/create-releases)
    (when write-report?
      (println (c/magenta "REPORT NOT IMPLEMENTED YET!")))))


(defn release!
  "Create a new release."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:help opts) (print-help)
    (:only-update-version opts) (release-impl :only-update-version opts)
    (:only-release opts) (release-impl :only-release opts)
    :else (print-help)))

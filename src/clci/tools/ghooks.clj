(ns clci.tools.ghooks
  "This module provides various helping funtions to work with git hooks.
  **Note**: Some of this code is based on https://blaster.ai/blog/posts/manage-git-hooks-w-babashka.html"
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [babashka.process :refer [shell sh]]
    [clci.conventional-commit :refer [valid-commit-msg?]]
    [clci.term :as c]
    [clci.tools.carve :refer [carve!]]
    [clci.tools.format :refer [format!]]
    [clci.tools.linter :refer [lint]]
    [clojure.string :as str]))


(defn hook-text
  "Build the content of a git hook file."
  [hook]
  (format "#!/bin/sh
# Installed by babashka task on %s

bb hooks --%s" (java.util.Date.) hook))


(defn spit-hook
  "Create a git hook file to run the bb hook method."
  [hook]
  (println (c/blue "Installing hook:") (c/yellow hook))
  (let [file (str ".git/hooks/" hook)]
    (spit file (hook-text hook))
    (fs/set-posix-file-permissions file "rwx------")
    (assert (fs/executable? file))))


(defn changed-files
  "Get a collection of all changed files."
  []
  (-> (shell {:out :string} "git --no-pager diff --name-only --no-color --cached --diff-filter=ACM")
      :out
      str/split-lines))


(def extensions
  "Extensions of files containing clojure code."
  #{"clj" "cljx" "cljc" "cljs" "edn"})


(defn- clj?
  "Test if the given filename is clojure code."
  [s]
  (when s
    (let [extension (last (str/split s #"\."))]
      (extensions extension))))


(def cli-options
  ""
  {:spec
   {:install			{:coerce :boolean :desc "Install git hooks (pre-commit, commit-msg)."}
    :pre-commit  	{:coerce :boolean :desc "Run the pre-commit hook function."}
    :commit-msg  	{:coerce :boolean :desc "Run the commit-msg hook function."}
    :help   			{:coerce :boolean :desc "Show help."}}})


(defn- print-help
  "Print help for the git hook task."
  []
  (println "Run a git hook.\n")
  (println (cli/format-opts cli-options)))


(defmulti hook-impl (fn [& args] (first args)))


;; Build the documentation
(defmethod hook-impl :install [& _]
  (spit-hook "pre-commit")
  (spit-hook "commit-msg"))


;; Git 'pre-commit' hook.
(defmethod hook-impl :pre-commit [& _]
  (println (c/blue "[pre-commit hook]"))
  (let [files (changed-files)]
    (format! {:fix true})
    (lint {})
    (carve! {:check true :report true})
    (doseq [file files]
      (sh (format "git add %s" file)))))


;; Git 'commit-msg' hook.
;; Takes the commit message and validates it conforms to the Conventional Commit specification
(defmethod hook-impl :commit-msg [& _]
  (let [commit-msg (slurp ".git/COMMIT_EDITMSG")
        msg-valid? (true? (valid-commit-msg? commit-msg))]
    (if msg-valid?
      (println (c/green "\u2713") " commit message follows the Conventional Commit specification")
      (do
        (println (c/red "\u2A2F") " commit message does NOT follow the Conventional Commit specification")
        (println (c/red "Abort commit!"))
        (System/exit -1)))))


;; Default handler to catch invalid arguments, print help
(defmethod hook-impl :default [& _]
  (print-help))


(defn hook
  "Implementation wrapper of the git hooks."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:install opts) (hook-impl :install)
    (:pre-commit opts) (hook-impl :pre-commit)
    (:commit-msg opts) (hook-impl :commit-msg)
    :else (hook-impl :help)))

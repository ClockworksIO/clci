(ns clci.tools.ghooks
  "This module provides various helping funtions to work with git hooks.
  **Note**: Some of this code is based on https://blaster.ai/blog/posts/manage-git-hooks-w-babashka.html"
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [babashka.process :refer [shell]]
    [clci.term :as c]
    [clojure.string :as str]))


(defn hook-text
  "Build the content of a git hook file."
  [hook]
  (format "#!/bin/sh
# Installed by babashka task on %s

bb clci run trigger git-%s --verbose" (java.util.Date.) hook))


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
  "Options available to use with the git-hook tool."
  {:spec
   {:install			{:coerce :boolean :desc "Install git hooks (pre-commit, commit-msg)."}
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


;; Default handler to catch invalid arguments, print help
(defmethod hook-impl :default [& _]
  (print-help))


(defn hook
  "Implementation wrapper of the git hooks."
  {:org.babashka/cli cli-options}
  [opts]
  (cond
    (:install opts) (hook-impl :install)
    :else (hook-impl :help)))

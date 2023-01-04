(ns clci.git-hooks-utils
  "This module provides various helping funtions to work with git hooks.
  **Note**: This code is based on https://blaster.ai/blog/posts/manage-git-hooks-w-babashka.html"
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [sh shell]]
    [clojure.string :as str]))


(defn hook-text
  "Build the content of a git hook file."
  [hook]
  (format "#!/bin/sh
# Installed by babashka task on %s

bb hooks %s" (java.util.Date.) hook))


(defn spit-hook
  "Create a git hook file to run the bb hook method."
  [hook]
  (println "Installing hook: " hook)
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


(defn clj?
  "Test if the given filename is clojure code."
  [s]
  (when s
    (let [extension (last (str/split s #"\."))]
      (extensions extension))))

(ns clci.repo
  "This module provides methods to read and update the repo.edn file."
  (:require
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]))


(defn pretty-spit
  [path data]
  (pprint data (clojure.java.io/writer path)))


(defn get-paths
  "TODO: combine with utils from monorepo!"
  []
  ["src"])


(defn read-repo
  "Read the repo configuration."
  []
  (-> (slurp "repo.edn")
      ;; TODO: Add checks that the mandatory fields are here!
      (edn/read-string)))


(defn update-version
  ""
  [version]
  (as-> (slurp "repo.edn") $
        (edn/read-string $)
        (assoc $ :version version)
        (pretty-spit "repo.edn" $)))

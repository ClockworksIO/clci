(ns clci.util
  "Utilities required by several modules of the project."
  (:require
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

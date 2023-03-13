(ns clci.util
  "Utilities required by several modules of the project."
  (:require
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


(defn- find-first-index-impl
  [coll pred idx]
  (cond
    (or (nil? coll) (empty? coll)) nil
    (pred (first coll)) idx
    :else (find-first-index-impl (rest coll) pred (inc idx))))


(defn find-first-index
  "Find the index in the collection `coll` where the predicate `pred` is
   true. Returns nil if no such item exist."
  [coll pred]
  (find-first-index-impl coll pred 0))


(defn find-first
  "Find and return the first element of `coll` where the `pred` is true."
  [pred coll]
  (first (filter pred coll)))

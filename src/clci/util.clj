(ns clci.util
  "Utilities required by several modules of the project."
  (:require
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]))



;;
;; Generic Utilities to work with (edn) files ;;
;;

(defn pretty-spit!
  "Write a clojure datastructure in an edn text file.
  Takes the `path` of the file and ther `data`. 
  **warning**: Will override the file's contents!"
  [path data]
  (binding [*print-namespace-maps* false]
    (pprint data (clojure.java.io/writer path))))


(defn slurp-edn
  "Read an edn file and parse the content as clojure datastructure."
  [path]
  (-> (slurp path)
      (edn/read-string)))


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


(defn map-on-map-values
  "Apply the given function `f` on all values of the map `m`."
  [m f]
  (reduce (fn [altered-map [k v]] (assoc altered-map k (f v))) {} m))


;; Taken from https://blog.mrhaki.com/2020/04/clojure-goodness-checking-predicate-for.html
(defn any
  "Test if the given predicate `pred` is true for at least one element of `coll`."
  [pred coll]
  ((comp boolean some) pred coll))


(defn str-split-first
  "Split the given string `s` using the regex `re` on the first occurence of `re`.
   See: https://stackoverflow.com/a/31146456/5841420"
  [re s]
  (clojure.string/split s re 2))


(defn str-split-last
  "Split the given string `s` using the regex `re` on the last occurence of `re`.
   See: https://stackoverflow.com/a/31146456/5841420"
  [re s]
  (let [pattern (re-pattern (str re "(?!.*" re ")"))]
    (str-split-first pattern s)))

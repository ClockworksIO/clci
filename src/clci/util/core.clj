(ns clci.util.core
  "Utilities required by several modules of the project."
  (:require
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.walk :refer [walk postwalk]]))



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


(defn slurp-env-file
  "Read a file in the .env format and provide the contents
   as a map with keyword keys."
  ([] (slurp-env-file ".repl.env"))
  ([path]
   (->> (slurp path)
        (str/split-lines)
        (map (fn [line] (str/split line #"=")))
        (map (fn [[key value]] [(keyword (str/trim key)) (str/trim value)]))
        (into {}))))


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


(defn mapv-r
  "Map and remove.
   Takes a collection `col` and maps over this collection applying the given
   function `f` on each element. Before adding the transformed element to the
   result collection, the predicate `p` is evaluated on the transformed value.
   If the predicate holds, the element is not added to the result collection."
  [f col p]
  (cond
    ;;
    (nil? col) nil
    ;; not a collection
    (not (coll? col)) (throw (ex-info "Invalid argument! The given `col` must be a collection." {}))
    ;; empty collection, no need to run a loop
    (empty? col) col
    ;; loop over the elements, apply f and remove elements after applying f where p holds
    :else
    (loop [head   (first col)
           tail   (rest col)
           acc    []]
      (if (nil? head)
        acc
        (let [el'   (f head)
              acc'  (if (p el')
                      acc
                      (conj acc el'))]
          (recur (first tail) (rest tail) acc'))))))


;; Taken from https://blog.mrhaki.com/2020/04/clojure-goodness-checking-predicate-for.html
(defn any
  "Test if the given predicate `pred` is true for at least one element of `coll`."
  [pred coll]
  (if-not (coll? coll)
    false
    ((comp boolean some) pred coll)))


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


(defn postwalk-reduce
  "Perform a postwalk and reduce the results of the visited nodes.
   Takes a function `f` and a `form` and an accumulating function
   `a-f`.
   Performs a depth-first, post-order traversal of `form` and 
   calls `f` on each sub-form. Uses f's return value in place of 
   the original. Applies `a-f` on the result of the recursively walked
   sub-form.
   This is similar to Clojure's `clojure.walk/postwalk` function 
   with the additional reducing step.
   The idea behind this function is to transform the given `form` and at
   the same time reducing the result of the recursive call into a single
   value."
  [f form a-f]
  (walk (comp #(apply a-f %) flatten (partial postwalk f)) f form))


(defn in?
  "true if coll contains elm."
  [coll elm]
  (some #(= elm %) coll))


(defn not-in?
  "true if coll does not contain elm."
  [coll elm]
  (not (in? coll elm)))


(defn same-elements?
  "Takes several collections and compares if they contain the same elements.
   The order of the elements in the collections does not matter. Duplicates are considered.
   !!! example
   
       ```clojure
       (same-elements? [1 1 2 3 4] [4 1 1 2 3]) ; -> true
       (same-elements? [1 2 3 4] [4 1 1 2 3]) ; -> false
       ```"
  [& colls]
  (apply = (map frequencies colls)))

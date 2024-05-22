(ns clci.templates.clojure
  "This module provides utilities for new products and bricks based
   on a Clojure template."
  (:require
    [babashka.fs :as fs]
    [clci.util.core :as u :refer [slurp-edn pretty-spit! join-paths]]))



;;
;; Utilities to work with deps.edn ;;
;;


(defn read-deps
  "Read a deps.edn file."
  ([] (read-deps "."))
  ([directory]
   (-> (fs/absolutize directory)
       str
       (join-paths "deps.edn")
       (slurp-edn))))


(defn write-deps!
  "Write the `data` to a deps.edn file.
   **warning**: Will override the file's contents!"
  ([data] (write-deps! "." data))
  ([directory data]
   (-> (fs/absolutize directory)
       str
       (join-paths "deps.edn")
       (pretty-spit! data))))


(defn deps-edn-base
  "Creates a base for a `deps.edn` file."
  []
  {:paths ["src"]
   :deps  {'org.clojure/clojure {:mvn/version "1.11.3"}}})


(defn with-alias
  "Add an alias with key `key` and the alias options `opts` to the
   given `deps-edn` configuration."
  [deps-edn key opts]
  (assoc-in deps-edn [:aliases key] opts))


(defn with-aliases
  "Add multiple aliases to the given `deps-edn`. Takes the `deps-edn` and
   a sequence of `aliases` where each alias in the sequence follows the 
   form `[alias-key alias-options]`."
  [deps-edn aliases]
  (reduce (fn [acc [alias-key alias-opts]]
            (with-alias acc alias-key alias-opts))
          deps-edn
          aliases))


;;
;; Utilities to work with bb.edn
;;

(defn read-bb
  "Read a bb.edn file."
  ([] (read-bb "."))
  ([directory]
   (-> (fs/absolutize directory)
       str
       (join-paths "bb.edn")
       (slurp-edn))))


(defn write-bb!
  "Write a `data` to the bb.edn file.
   **warning**: Will override the file's contents!"
  ([data] (write-bb! "." data))
  ([directory data]
   (-> (fs/absolutize directory)
       str
       (join-paths "bb.edn")
       (pretty-spit! data))))


(defn bb-base
  "Creates a base for a `deps.edn` file."
  []
  {:paths ["bb"]
   :min-bb-version  "1.3.190"})


(defn add-bb-task
  "Add a new task to the bb.edn file.
  Takes the `name` which must be a string than can be cast to a symbol
  and identifies the babashka task and the function `t-fn` that is executed
  from the task. Also takes an optional `description` string for 
  the task and an optional list of requirements fo the task.
  
  The function that is executed by the task will be wrapped in a babashka
  (exec t-fn) statement and therefore must be fully qualified. It also should
  take an opts argument and implement the babashka cli API to parse optional
  arguments."
  [{:keys [name t-fn desc reqs]}]
  (-> (read-bb)
      (assoc-in
        [:tasks (symbol name)]
        (cond-> {:task (list 'exec t-fn)}
          (some? desc) (assoc :desc desc)
          (some? reqs) (assoc :requires reqs)))
      (write-bb!)))



(def available-aliases
  "The aliases available to add to a deps.edn file using the Assistant."
  [{:name "nrepl" :key :nREPL} {:name "clj-format" :key :format}])


;; Method to add an alias to a new product (Clojure products only!)
;; Works only with pre-defined aliases.
(defmulti alias-opts (fn [alias] alias))


;; Fallback for unknown aliases, they just get ignored
(defmethod alias-opts :default [_] {})


;; Add the option to run an nREPL server for the product during development
(defmethod alias-opts :nREPL [_] {:extra-deps {'nrepl/nrepl {:mvn/version "1.1.0"}}})


;; Add Clojure code formatting using cljstyle. Can be run using a clci Action
(defmethod alias-opts :format [_] {:deps {'mvxcvi/cljstyle {:mvn/version "0.15.0"}}})

(ns clci.products
  (:require
    [babashka.fs :as fs]
    [clci.repo :as repo]
    [clci.util.core :as u :refer [slurp-edn pretty-spit! join-paths]]
    [clojure.string :as str]))


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


(defn- deps-edn-base
  "Creates a base for a `deps.edn` file."
  []
  {:paths ["src"]
   :deps  {'org.clojure/clojure {:mvn/version "1.11.3"}}})


(defn- with-alias
  "Add an alias with key `key` and the alias options `opts` to the
   given `deps-edn` configuration."
  [deps-edn key opts]
  (assoc-in deps-edn [:aliases key] opts))


(defn- with-aliases
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


(defn- bb-base
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


;; Create a new product based on its `kind` (i.e. :app or :library)
;; and a template (i.e. :clojure)
(defmulti create-product! (fn [kind template opts] template))


;; Create a new product using the clojure template.
;; Creates the required directory structure and adds a deps.edn and bb.edn
;; file with a base configuration
(defmethod create-product! :clojure [kind _ product-opts]
  (let [product-entry     (-> (select-keys product-opts [:name :root :key :version :no-release?])
                              (assoc :type kind))
        product-name      (:name product-opts)
        deps-edn          (-> (deps-edn-base)
                              (with-aliases (:aliases product-opts)))
        product-root      (str (fs/absolutize (:root product-opts)))
        clj-user-dir      (join-paths product-root "dev")
        default-src-dir   (join-paths product-root "src" (str/replace product-name #"\-" "_"))]
    (fs/create-dirs product-root)
    (fs/create-dirs clj-user-dir)
    (fs/create-dirs default-src-dir)
    (write-deps! product-root deps-edn)
    (write-bb! product-root (bb-base))
    (spit (join-paths clj-user-dir "user.clj") "(ns user)")
    (spit (join-paths default-src-dir "core.clj") (format "(ns %s.core)" product-name))
    (repo/add-product! product-entry)))


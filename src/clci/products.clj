(ns clci.products
  (:require
    [babashka.fs :as fs]
    [clci.repo :as repo]
    [clci.templates.clojure :refer [deps-edn-base with-aliases bb-base write-bb! write-deps!]]
    [clci.util.core :as u :refer [join-paths]]
    [clojure.string :as str]))


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

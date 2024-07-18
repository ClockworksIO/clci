(ns clci.bricks
  "This module provides the tools to work with Bricks."
  (:require
    [babashka.fs :as fs]
    [clci.repo :as repo]
    [clci.templates.clojure :refer [deps-edn-base with-aliases bb-base write-bb! write-deps!]]
    [clci.util.core :as u :refer [join-paths]]
    [clojure.string :as str]))



;; Create a new Brick using the specified template
(defmulti create-brick! (fn [template opts] template))


;; Create a new Clojure Brick
;; Creates the required directory structure and adds a deps.edn and bb.edn
;; file with a base configuration
(defmethod create-brick! :clojure [_ brick-opts]
  (let [brick-entry     (select-keys brick-opts [:name :key :version])
        brick-name      (:name brick-opts)
        deps-edn        (-> (deps-edn-base)
                            (with-aliases (:aliases brick-opts)))
        brick-root      (-> (join-paths "bricks" brick-name)
                            fs/absolutize
                            str)
        clj-user-dir      (join-paths brick-root "dev")
        default-src-dir   (join-paths brick-root "src" "brick" (str/replace brick-name #"\-" "_"))]
    (fs/create-dirs brick-root)
    (fs/create-dirs clj-user-dir)
    (fs/create-dirs default-src-dir)
    (write-deps! brick-root deps-edn)
    (write-bb! brick-root (bb-base))
    (spit (join-paths clj-user-dir "user.clj") "(ns user)")
    (spit (join-paths default-src-dir "core.clj") (format "(ns brick.%s.core)" brick-name))
    (spit (join-paths default-src-dir "impl.clj") (format "(ns brick.%s.impl)" brick-name))
    (repo/add-brick! brick-entry)))

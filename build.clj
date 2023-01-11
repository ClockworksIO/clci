(ns build
  "Build definitions of the project."
  (:require
    [clojure.edn :as edn]
    [clojure.tools.build.api :as b]))


;; the deps.edn file provides the necessary configuration for the builds
(def project (-> (slurp "deps.edn") (edn/read-string)))
(def build-conf (:build project))

(def lib (get-in project [:build :lib-name]))
(def version (get-in project [:build :version]))
(def target-dir (get-in project [:build :target-dir]))
(def class-dir (format "%s/classes" target-dir))
(def src-dirs (:paths project))

(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))


(defn clean!
  "Clean all build artifacts."
  [_]
  (b/delete {:path "target"}))


(defn uberpod
  "Assemble an uberjar from the project to build a babashka pod."
  [_]
  ;(clean! nil)
  (b/copy-dir {:src-dirs    src-dirs
               :target-dir  class-dir})
  (b/compile-clj {:basis    basis
                  :src-dirs   src-dirs
                  :class-dir  class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'clci.main}))

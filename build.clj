(ns build
  "Build definitions of the project."
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.tools.build.api :as b]))


;; the deps.edn file provides the necessary configuration for the builds
(def project (-> (slurp "deps.edn") (edn/read-string)))

(def lib (:lib-name project))
(def pod-main (:pod-main project))
(def version (:version project "0.0.0-snapshot"))
(def target-dir (:target-dir project "target"))
(def resources-dir (:resources-dir project "resources"))
(def class-dir (format "%s/classes" target-dir))
(def target-resources-dir (format "%s/resources" target-dir))
(def src-dirs (:paths project))


(def pod-manifest
  (format "%s/%s/pod-manifest.edn" (-> lib namespace (str/replace "." "/")) (name lib)))


(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "%s/%s-%s-%s-standalone.jar" target-dir (-> lib namespace (str/replace "." "-"))  (name lib) version))


(defn clean!
  "Clean all build artifacts."
  [_]
  (b/delete {:path target-dir}))


(defn uberpod
  "Assemble an uberjar from the project to build a babashka pod."
  [_]
  ;; (clean! nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis})
  (b/copy-dir {:src-dirs    src-dirs
               :target-dir  class-dir})
  (b/copy-file {:src    (str resources-dir "/" pod-manifest)
                :target (str target-resources-dir "/" pod-manifest)})
  (b/compile-clj {:basis    basis
                  :src-dirs   src-dirs
                  :class-dir  class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main pod-main}))

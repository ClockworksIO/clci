(ns build
  "THis module provides various tasks to build the project."
  (:require
    [babashka.process :refer [shell sh]]
    [clojure.edn :as edn]
    [clojure.term.colors :as c]))


(def build-conf
  "Load the build configuration from the projects `deps.edn` file."
  (-> (slurp "deps.edn") (edn/read-string) :build))


;; Method to build the module for various targets.
(defmulti build (fn [& args] (first args)))


;; Cleanup all build artifacts.
(defmethod build "clean" [& _]
  (-> (sh "clj -T:build clean!") :out println))


;; Build a pod protocol compatible binary using Clojure and GraalVM. 
;; The pod can be used from any babashka script.
(defmethod build "pod" [& _]
  (let [native-image  (System/getenv "NATIVE_IMAGE")
        target-dir    (:target-dir build-conf)
        app-name      (name (:lib-name build-conf))
        version       (:version build-conf)
        uber-file     (format "%s/%s-%s-standalone.jar" target-dir app-name version)]
    (if-not native-image
      (println (c/red "Error: $NATIVE_IMAGE is not set!"))
      (do
        (println (c/blue "Building uberjar..."))
        (-> (sh "clj -T:build uberpod") :out :println)
        (println (str (c/green "\u2713") (c/green " uberjar build successful.")))
        (shell native-image
               "-jar" uber-file
               (format "-H:Name=%s" app-name)
               "-H:+ReportExceptionStackTraces"
               "--report-unsupported-elements-at-runtime"
               "--initialize-at-build-time"
               ;; "--verbose"
               "--no-fallback"
               "--no-server"
               "--static"
               (format "%s/%s-%s.main" target-dir app-name version))))))


;; Default handler to catch unknown build targets.
(defmethod build :default [& args]
  (println (c/yellow "Unknown build target:") (c/red (first args))))

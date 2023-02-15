(ns build
  "This module provides various tasks to build the project."
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [shell sh]]
    [clci.proc-wrapper :refer [wrap-process]]
    [clci.term :refer [red blue green yellow]]
    [clojure.edn :as edn]
    [clojure.string :as str]))


(def project
  "Load the build configuration from the projects `deps.edn` file."
  (-> (slurp "deps.edn") (edn/read-string)))


(def static-build-config
  "Load the build configuration for the static tool build from `./cli-tool/deps.edn` file."
  (-> (slurp "./cli-tool/deps.edn") (edn/read-string)))


(defn executable-name
  "Generate a repeatable and clean name for the executable."
  [main version os-name os-arch]
  (format "%s-%s-%s-%s"
          version
          (str/replace (str/lower-case os-name) #"[\.\*]" "")
          (str/lower-case os-arch)
          main))


;; Method to build the module for various targets.
(defmulti build (fn [& args] (first args)))


;; Cleanup all build artifacts.
(defmethod build "clean" [& _]
  (println (blue "[1/1] Removing all build artifacts..."))
  (fs/delete-tree (:target-dir project))
  (println (green "\u2713 done.")))


;; Build an uberjar from for the clci cli tool.
(defmethod build "uberjar" [& _]
  (println (blue "[1/1] Building uberjar..."))
  (wrap-process [{:inherit true :dir "cli-tool"} "clj -T:build uberjar"])
  (println (green "\u2713 uberjar build successful.")))


;; Build a static binary for the clci cli tool.
(defmethod build "static" [_ & {:as args}]
  (let [target-os     (get args ":os" "Linux.*")
        target-arch   (get args ":arch" "amd64")
        native-image  (System/getenv "NATIVE_IMAGE")
        target-dir    (:target-dir project)
        lib           (:lib-name project)
        app-name      (name lib)
        version       (:version project)
        uber-file     (format "%s/%s-%s-%s-standalone.jar" target-dir (-> lib namespace (str/replace "." "-"))  (name lib) version)]
    ;; Check if the NATIVE_IMAGE path is set
    (when-not native-image
      (println (red "Error: $NATIVE_IMAGE is not set!"))
      (System/exit -1))
    ;; Build the uberjar required to build the native binary
    (println (blue "[1/2] Building uberjar..."))
    (-> (shell {:out :string :dir "cli-tool"} "clj -T:build uberjar") :out :println)
    (println (green "\u2713 uberjar build successful."))
    ;; Build the binary with GraalVM using native-image
    (println (blue "[2/2] Building binary with GraalVM..."))
    (shell native-image
           "-jar" uber-file
           (format "-H:Name=%s" app-name)
           "-H:+ReportExceptionStackTraces"
           "--report-unsupported-elements-at-runtime"
           "--initialize-at-build-time"
           "--no-fallback"
           "--no-server"
           "--static"
           (format "%s/%s" target-dir (executable-name (name (:main static-build-config)) (:version project) target-os target-arch)))
    (println (green "\u2713 Build successful."))))


;; Default handler to catch unknown build targets.
(defmethod build :default [& args]
  (println (yellow "Unknown build target:") (red (first args))))

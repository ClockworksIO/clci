(ns build
  "This module provides various tasks to build the project."
  (:require
    [babashka.process :refer [shell sh]]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.term.colors :as c]))


(def project
  "Load the build configuration from the projects `deps.edn` file."
  (-> (slurp "deps.edn") (edn/read-string)))


(defn executable-name
  "Generate a repeatable and clean name for the executable."
  [pod-main version os-name os-arch]
  (format "v%s-%s-%s-%s"
          version
          (str/replace (str/lower-case os-name) #"[\.\*]" "")
          (str/lower-case os-arch)
          pod-main))


(defn update-pod-manifest!
  "Create the pod-manifest.edn based on the template in the resources dir."
  [pod-manifest-template-path]
  (let [template  (-> (slurp pod-manifest-template-path) (edn/read-string))]
    (as-> template $
          (assoc $
                 :pod/name (:lib-name project)
                 :pod/description (:description project)
                 :pod/version (:version project)
                 :pod/license (get-in project [:license :name])
                 :pod/artifacts (mapv
                                  (fn [artifact]
                                    {:os/name (:os/name artifact)
                                     :os/arch (:os/arch artifact)
                                     :artifact/url (format
                                                     (str (:url project) "/archive/refs/tags/v%s.zip")
                                                     (:version project)
                                                     (:os/name artifact)
                                                     (:os/arch artifact))
                                     :artifact/executable (executable-name
                                                            (name (:pod-main project))
                                                            (:version project)
                                                            (:os/name artifact)
                                                            (:os/arch artifact))})
                                  (:pod-artifacts project)))
          (spit pod-manifest-template-path $))))


;; Method to build the module for various targets.
(defmulti build (fn [& args] (first args)))


;; Cleanup all build artifacts.
(defmethod build "clean" [& _]
  (-> (sh "clj -T:build clean!") :out println))


(defn valid-pod-target?
  "Takes the Pod's target operating system and architecture and checks if
  they are a valid combination."
  [os arch]
  (cond
    (and (= os "Linux.*") (= arch "amd64"))
    true
    :else
    false))


;; Build a pod protocol compatible binary using Clojure and GraalVM. 
;; The pod can be used from any babashka script.
(defmethod build "pod" [_ & {:as args}]
  (let [target-os     (get args ":os" "Linux.*")
        target-arch   (get args ":arch" "amd64")
        native-image  (System/getenv "NATIVE_IMAGE")
        target-dir    (:target-dir project)
        resources-dir (:resources-dir project)
        lib           (:lib-name project)
        app-name      (name lib)
        version       (:version project)
        pod-manifest  (format "%s/%s/%s/pod-manifest.edn" resources-dir (-> lib namespace (str/replace "." "/")) app-name)
        uber-file     (format "%s/%s-%s-%s-standalone.jar" target-dir (-> lib namespace (str/replace "." "-"))  (name lib) version)]
    ;; Check if target os and architecture are valid
    ;; TODO: add extra check if the current platform this process is running on matches the requested os and architecture!
    (when-not (valid-pod-target? target-os target-arch)
      (println (c/red "\u2A2F") (c/red " combination of target OS and Architecture not allowed!"))
      (System/exit -1))
    ;; Check if the NATIVE_IMAGE path is set
    (when-not native-image
      (println (c/red "Error: $NATIVE_IMAGE is not set!"))
      (System/exit -1))
    ;; Write pod manifest
    (println (c/blue "[1/3] Updating Pod manifest..."))
    (update-pod-manifest! pod-manifest)
    (println (str (c/green "\u2713") (c/green " update successful.")))
    ;; Build the uberjar required to build the native binary
    (println (c/blue "[2/3] Building uberjar..."))
    (-> (sh "clj -T:build uberpod") :out :println)
    (println (str (c/green "\u2713") (c/green " uberjar build successful.")))
    ;; Build the binary with GraalVM using native-image
    (println (c/blue "[3/3] Building binary with GraalVM..."))
    (shell native-image
           "-jar" uber-file
           (format "-H:Name=%s" app-name)
           "-H:+ReportExceptionStackTraces"
           "--report-unsupported-elements-at-runtime"
           "--initialize-at-build-time"
           "--no-fallback"
           "--no-server"
           "--static"
           (format "%s/%s" target-dir (executable-name (name (:pod-main project)) (:version project) target-os target-arch)))
    (println (str (c/green "\u2713") (c/green " Build successful.")))))


;; Default handler to catch unknown build targets.
(defmethod build :default [& args]
  (println (c/yellow "Unknown build target:") (c/red (first args))))

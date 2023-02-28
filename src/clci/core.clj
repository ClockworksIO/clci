(ns clci.core
  "This module provides the means to install clci in any project and run
  a task to manage that project."
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [clci.repo :as rp]
    [clci.term :refer [blue red]]))


(def cli-args
  "")


(defn clci
  "Handler for the clci task installed in a project."
  [& args]
  (println "hello from clci"))


(defn install-in-project
  "Install clci in the current project."
  [args]
  (println (blue "Setting up clci in the existing project."))
  ;; check if required files exist, if not create them with empty content
  (when-not (fs/exists? "bb.edn")
    (rp/pretty-spit! "bb.edn" {}))
  (when-not (fs/exists? "deps.edn")
    (rp/pretty-spit! "deps.edn" {}))
  (when-not (fs/exists? "repo.edn")
    (rp/pretty-spit! "repo.edn" {}))
  (let [opts (:opts args)
        bb-edn    (rp/read-bb)
        deps-edn  (rp/read-deps)]
    ;; setup the repo.edn minimal base
    ;; update bb.edn with the clci task
    (-> bb-edn
        ;; add
        (assoc-in [:tasks 'clci] '{:doc  "Run clci."
                                   :requires    ([clci.core :as clci])
                                   :task (exec 'clci/clci)})
        (rp/write-bb!))
    (-> (rp/repo-base (select-keys opts [:scm :scm-provider :scm-repo-name :scm-repo-owner]))
        (rp/with-single-project (select-keys opts [:initial-version]))
        (rp/write-repo!))))


(defn- handle-main-input-errors
  "Handle errors caused by invalid input passed to the main function.
  Show a useful error message to the user."
  [err]
  (case (:reason (ex-data err))
    :invalid-initial-version (println (red "\u2A2F") " The initial version must follow the Semantic Versioning Specification!")))


(defn -main
  "Main entry point, run to install clci in a project."
  [& args]
  (try
    (cli/dispatch
      [{:cmds ["install"]
        :fn install-in-project
        :spec
        {:scm             {:coerce :keyword :require true :desc "Name of the scm used. Only supports 'git' right now."}
         :scm-provider    {:coerce :keyword :require true :desc "Provider of the repository service. Only supports 'github' right now."}
         :scm-repo-name   {:coerce :string :require true :desc "Name of the repository."}
         :scm-repo-owner  {:coerce :string :require true :desc "Owner (i.e. user or organization) of the repository."}
         :single-repo     {:coerce :boolean :desc "Set to true if the repository contains only a single project at its root. This will setup the repo.edn configuration accordingly."}
         :initial-version {:coerce :string :desc "Optional version used to set for the project. Only applicable in combination with the `--single-repo` option."}}}]

      args)
    (catch clojure.lang.ExceptionInfo ex (handle-main-input-errors ex))))


(comment
  "bb -m clci.core install --scm git --scm-provider github --scm-repo-name example --scm-repo-owner superman --single-repo")


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

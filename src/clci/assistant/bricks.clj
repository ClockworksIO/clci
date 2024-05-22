(ns clci.assistant.bricks
  "This module provides an Assistant for Bricks."
  (:require
    [clci.assistant.dialog :as dialog]
    [clci.bricks :refer [create-brick!]]
    [clci.repo :as repo]
    [clci.templates.clojure :as cljt]
    [clci.term :refer [red green yellow]]))


(def add-brick-dialog
  "A dialog used by the assistant to add a new Brick to the repo."
  [{:step     :select-template-label
    :element  :text
    :text     "Please select a template for the Brick:"}
   {:step     :select-template
    :element  :choose
    :filter   (fn [_ options]
                options)
    :options  [{:name "Clojure" :key :clojure}]}
   {:step     :brick-name-label
    :element  :text
    :text     "Please specify the name of the Brick."}
   {:step         :brick-name
    :element      :input
    :placeholder  "<brick-name>"}
   {:step     :brick-nondefault-key-label
    :element  :text
    :format   (fn [_ history]
                (format "Would you like to specify a custom Brick key (default is \"%s\")?" (keyword (:input (dialog/find-step history :brick-name)))))}
   {:step     :brick-nondefault-key-select
    :element  :choose
    :options  [{:name "No" :key :no}
               {:name "Yes" :key :yes}]}
   {:step     :brick-key-label
    :element  :text
    :skip?    (fn [_ history]
                (if (= :no (dialog/find-in-step history :brick-nondefault-key-select :selected-options first :key))
                  1
                  0))
    :text     "Please specify the key of the new Brick."}
   {:step         :brick-key
    :element      :input
    :post-fn      keyword
    :placeholder  "<brick-key>"}
   {:step     :add-clojure-aliases-label
    :element  :text
    :text     "Would you like to add aliases to your Clojure Brick?"}
   {:step     :add-clojure-aliases-select
    :element  :choose
    :limit    2
    :options  cljt/available-aliases}])



(defn run-add-brick-assistant
  "Run the brick assistant and add a new brick to the
   repository using the data a user has provided during the
   assistant dialog."
  []
  (when-not (repo/valid-repo?)
    (print (red "Unable to run the assistant.\n"))
    (print (yellow "The current directory does not has a valid repo.edn configuration file!\n"))
    (flush)
    (System/exit 1))
  (print (green "repo configuration is valid.\n"))
  (flush)
  (let [user-input        (dialog/run-linear-dialog  add-brick-dialog)
        brick-template    (dialog/find-in-step user-input :select-template :selected-options first :key)
        selected-aliases  (mapv :key (dialog/find-in-step user-input :add-clojure-aliases-select :selected-options))
        brick-opts        {:name        (dialog/find-in-step user-input :brick-name :input)
                           :key         (or (dialog/find-in-step user-input :brick-key :input) (keyword (dialog/find-in-step user-input :brick-name :input)))
                           :version     "0.0.0"
                           :aliases     (mapv (fn [a-key] [a-key (cljt/alias-opts a-key)]) selected-aliases)}]
    (create-brick! brick-template brick-opts)
    (println (green "New Brick created."))))


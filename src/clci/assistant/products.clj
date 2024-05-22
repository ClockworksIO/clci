(ns clci.assistant.products
  "This module provides an assistant to work with products.
   For example the assistant can be used to add a new product to the
   repository and setting up the product's base using a template."
  (:require
    [clci.assistant.dialog :as dialog]
    [clci.products :refer [create-product!]]
    [clci.repo :as repo]
    [clci.templates.clojure :as cljt]
    [clci.term :refer [red green yellow]]
    [clojure.string :as str]))


(def add-product-dialog
  "A dialog used by the assistant to add a new product to the repo."
  [{:step       :welcome-msg
    :element    :text
    :text       (str/join
                  "\n"
                  ["You are about to create a new product in your repository."
                   "This assistant will guide you through the steps."])
    :linebreak? true}
   {:step     :wait-before-start
    :element  :wait
    :seconds  1}
   {:step     :product-type-question-label
    :element  :text
    :text     "Which type of product would you like to add?"}
   {:step     :select-product-type
    :element  :choose
    :options  [{:name "Application" :key :application} {:name "Library" :key :library} {:name "Other" :key :other}]}
   {:step     :select-template-label
    :element  :text
    :text     "Please select a template for your new product:"}
   {:step     :select-template
    :element  :choose
    :filter   (fn [_ options]
                options)
    :options  [{:name "Clojure" :key :clojure}
               ;; {:name "Babashka" :key :babashka}
               ;; {:name "ClojureScript" :key :clojurescript}
               ;; {:name "Nbb" :key :nbb}
               ]}
   {:step     :product-name-label
    :element  :text
    :text     "Please specify the name of the new product."}
   {:step         :product-name
    :element      :input
    :placeholder  "<product-name>"}
   {:step     :product-nondefault-root-label
    :element  :text
    :format   (fn [_ history]
                (format "Would you like to use a different product root directory (default is \"./%s/\")?" (:input (dialog/find-step history :product-name))))}
   {:step     :product-nondefault-root-select
    :element  :choose
    :options  [{:name "No" :key :no}
               {:name "Yes" :key :yes}]}
   {:step     :product-root-label
    :element  :text
    :skip?    (fn [_ history]
                (if (= :no (dialog/find-in-step history :product-nondefault-root-select :selected-options first :key))
                  1
                  0))
    :text     "Please specify the root directory of the new product."}
   {:step         :product-root
    :element      :input
    :placeholder  "<product-root>"}
   {:step     :product-nondefault-key-label
    :element  :text
    :format   (fn [_ history]
                (format "Would you like to specify a custom product key (default is \"%s\")?" (keyword (:input (dialog/find-step history :product-name)))))}
   {:step     :product-nondefault-key-select
    :element  :choose
    :options  [{:name "No" :key :no}
               {:name "Yes" :key :yes}]}
   {:step     :product-key-label
    :element  :text
    :skip?    (fn [_ history]
                (if (= :no (dialog/find-in-step history :product-nondefault-key-select :selected-options first :key))
                  1
                  0))
    :text     "Please specify the key of the new product."}
   {:step         :product-key
    :element      :input
    :post-fn      keyword
    :placeholder  "<product-key>"}
   {:step     :product-no-release?-label
    :element  :text
    :text     "Do you want to create distinct releases for the product automatically?"}
   {:step     :product-no-release?-select
    :element  :choose
    :options  [{:name "No" :key :no}
               {:name "Yes" :key :yes}]}
   {:step     :use-clci-actions-label
    :element  :text
    :text     "Would you like to add aliases to your product to use the following Actions?"}
   {:step     :select-action-aliases-select
    :element  :choose
    :limit    2
    :options  cljt/available-aliases}])



(defn run-add-product-assistant
  "Run the product assistant and add a new product to the
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
  (let [user-input        (dialog/run-linear-dialog  add-product-dialog)
        product-type      (dialog/find-in-step user-input :select-product-type :selected-options first :key)
        product-template  (dialog/find-in-step user-input :select-template :selected-options first :key)
        selected-aliases  (mapv :key (dialog/find-in-step user-input :select-action-aliases-select :selected-options))
        product-opts      {:name        (dialog/find-in-step user-input :product-name :input)
                           :root        (or (dialog/find-in-step user-input :product-root :input) (dialog/find-in-step user-input :product-name :input))
                           :key         (or (dialog/find-in-step user-input :product-key :input) (keyword (dialog/find-in-step user-input :product-name :input)))
                           :version     "0.0.0"
                           :no-release? (not= :yes (dialog/find-in-step user-input :product-no-release?-select :selected-options first :key))
                           :aliases     (mapv (fn [a-key] [a-key (cljt/alias-opts a-key)]) selected-aliases)}]
    (create-product! product-type product-template product-opts)
    (println (green "New product created."))))


(comment
  ;; run the product assistant
  (run-add-product-assistant)
  )

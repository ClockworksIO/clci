(ns user
  ""
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [sh shell]]
    [bblgum.core :refer [gum]]
    [clci.assistant.dialog :as dialog]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.release :as rel]
    [clci.repo :as repo]
    [clci.term :refer [blue red green yellow grey white cyan]]
    [clci.util.core :refer [in?]]
    [clojure.core :as c]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))


;; Dialog Stuff


(def add-product-dialog
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
    :options  [{:name "App" :key :app} {:name "Library" :key :library} {:name "Other" :key :other}]}
   {:step     :select-template-label
    :element  :text
    :text     "Please select a template for your new product:"}
   {:step     :select-template
    :element  :choose
    :filter   (fn [_ options]
                options)
    :options  [{:name "Clojure" :key :clojure}
               {:name "Babashka" :key :babashka}
               {:name "ClojureScript" :key :clojurescript}
               {:name "Nbb" :key :nbb}]}
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
                  2
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
                  2
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
   {:step     :select-action-aliases
    :element  :choose
    :limit    2
    :options  [{:name "kondo" :key :kondo} {:name "clj-format" :key :clj-format}]}])


(defn with-new-product
  "Add a new `product` to the given `repo`."
  [repo product]
  (update repo :products #(conj product)))


;; goes in repo.edn
(defn add-product!
  "Add a new product to the repo configuration.
   Throws an exception if the given `product` violates the product specs."
  [product]
  ;; (when-not (repo/valid-repo? repo)
  ;;  (ex-info "Repo configuration missing or invalid!" {:cause ::invalid-repo-configuration}))
  (when-not (repo/valid-product? product)
    (ex-info "Product specification invalid!" {:cause ::invalid-product-specification}))
  (repo/write-repo! (with-new-product (repo/read-repo) product)))


(defn run-add-product-assistant
  ""
  []
  (let [repo (repo/read-repo)]
    (when-not (repo/valid-repo?)
      (print (red "Unable to run the assistant.\n"))
      (print (yellow "The current directory does not has a valid repo.edn configuration file!\n"))
      (flush)
      (System/exit 1))
    (print (green "repo configuration is valid.\n"))
    (flush)
    (let [user-input      (dialog/run-linear-dialog  add-product-dialog)
          new-product     {:name        (dialog/find-in-step user-input :product-name :input)
                           :root        (or (dialog/find-in-step user-input :product-root :input) (dialog/find-in-step user-input :product-name :input))
                           :key         (or (dialog/find-in-step user-input :product-key :input) (keyword (dialog/find-in-step user-input :product-name :input)))
                           :version     "0.0.0"
                           :no-release? (not= :yes (dialog/find-in-step user-input :product-no-release?-select :selected-options first :key))}]
      new-product)))



(run-add-product-assistant)


(def foo
  [{:step :welcome-msg}
   {:step :wait-before-start, :failure? false}
   {:step :product-type-question-label}
   {:step :select-product-type, :selected-options '({:name "App", :key :app})}
   {:step :select-template-label}
   {:step :select-template, :selected-options '({:name "Clojure", :key :clojure})}
   {:step :product-name-label}
   {:step :product-name, :input "dd"}
   {:step :product-nondefault-root-label}
   {:step :product-nondefault-root-select, :selected-options '({:name "No", :key :no})}
   {:step :product-root, :input nil}
   {:step :use-clci-actions-label}
   {:step :select-action-aliases, :selected-options '({:name "kondo", :key :kondo})}])


(dialog/find-in-step foo :product-name)
(dialog/find-in-step foo :product-name :input)

(dialog/find-in-step foo :product-nondefault-root-select :selected-options)

(dialog/find-in-step foo :product-nondefault-root-select :selected-options first)
(dialog/find-in-step foo :product-nondefault-root-select :selected-options first :key)

(dialog/find-in-step foo :product-nondefault-root-select :selected-options 0)
(dialog/find-in-step foo :product-nondefault-root-select :selected-options 0 :name)

(= :no (dialog/find-in-step foo :product-nondefault-root-select :selected-options first :key))


;; (run-linear-dialog linear-dialog)

;; (find-step [{:step :welcome-msg}
;;             {:step :wait-before-start, :failure? false}
;;             {:step :product-type-question-label}
;;             {:step :select-product-type, :selected-options '({:name "App", :key :app})}
;;             {:step :use-clci-actions-label}
;;             {:step :select-action-aliases, :selected-options ({:name "kondo", :key :kondo} {:name "clj-format", :key :clj-format})}
;;             {:step :select-template-label}
;;             {:step :select-template, :selected-options '({:name "Clojure", :key :clojure})}]
;;            :select-action-aliases)


;;

(defn create-empty-library
  ""
  [root use-kondo? use-cljformat?])


(defn add-product
  "Add a new product to the repo."
  [{:keys [root key kind initial-version no-release?] :or {no-release? false initial-version "0.0.0"}}]
  ;; (if-not (and
  ;;           (s/valid? :clci.repo.product/root root)
  ;;           (s/valid? :clci.repo.product/key key)
  ;;           (s/valid? :clci.repo.product/version initial-version)
  ;;           (s/valid? :clci.repo.product/root root))
  (let [current-repo  (repo/read-repo)
        new-product   {:root        root
                       :key         key
                       :type        kind
                       :version     initial-version
                       :no-release? no-release?}
        repo'         (update-in current-repo [:products] (fn [old] (conj old new-product)))]
    (when (= :clojure.spec.alpha/invalid (s/conform :clci.repo/product new-product))
      (throw
        (ex-info "The product specification does not conform to spec!"
                 {})))
    ;; (fs/create-dir root)
    ;; (repo/write-repo! repo')
    ))



(comment
  "1. Create a directory for the repository.
   2. Add a `bb.edn` file with the following content:
   ```clojure
   {:deps  {clockworksio/clci   {:git/url \"https://github.com/clockworksio/clci\"
                                 :git/sha \"<latest-hash>\"}}
    :tasks {clci                {:doc \"Run clci.\",
                                 :task (exec 'clci.core/-main)}}
   }
   ```
   3. Run `bb clci setup`
   
   ")

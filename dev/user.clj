(ns user
  ""
  (:require
    [clci.util.core :refer [in?]]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.release :as rel]
    [clci.repo :as repo]
    [clci.term :refer [blue red green yellow grey white cyan]]
    [clojure.core :as c]
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [babashka.process :refer [sh shell]]
    [babashka.fs :as fs]
    [bblgum.core :refer [gum]]))



(defn installed?
  "Test if gum is installed on the machine."
  []
  (try 
    (shell {:out :string :err :string} "gum --version")
    true
    (catch Exception e
     false
      )))


(defn- shell-exit-with-error?
  "Test if the given `exit-code` indicates a shell process did exit with an error."
  [exit-code]
  (not= exit-code 0))

(defn- yield-failure
  "Yield an error map for a dialog `step` when a `failure?` occured."
  [failure? step]
  {:step      step
   :failure?  failure?})

(defn- start-in-new-line
  "Begin terminal output on a new line."
  []
  (print "\n"))

(defn- print-welcome
  "Print the clci assistant welcome message on the terminal."
  []
  (print "Welcome to the" (yellow "clci") "setup assistant.\n"))


(defmulti render (fn [{:keys [element]} _] element))


(defmethod render :text [elem history]
  (if (:linebreak? elem)
    (print (str (:text elem) "\n"))
    (print (:text elem)))
  {:step (:step elem)}
 )

(defmethod render :choose [elem history]
  (let [filter-fn               (get elem :filter #(fn [_ options] options))
        options                 (filter-fn history (:options elem))
        limit                   (get elem :limit 1)
        step                    (:step elem)
        {:keys [status result]} (gum :choose (mapv :name options) :limit limit)]
    (if (= status 0)
      {:step step
       :selected-options (filter (fn [opt] (in? result (:name opt))) options)}
      {:step      step
       :failure?  true})))

;(gum :choose ["A" "B"] :limit 1)


(defmethod render :wait [elem history]
 (-> (shell {:out :string} (format "sleep %s" (get elem :seconds 0))) 
   :exit 
   (shell-exit-with-error?)
   (yield-failure (:step elem))
   )
 )


(defn run-linear-dialog
  ""
  [dialog]
  (start-in-new-line)
  (print-welcome)
  (loop [head     (first dialog)
         tail     (rest dialog)
         history  []]
      (cond 
        ;; no more items left
        (nil? head)
        history
        ;; error on the previous item
        (-> history last :failure?)
        (ex-info "Unable to execute full dialog!" {:history history})
        ;; otherwise continue with the next element
        :else
        (recur (first tail) (rest tail) (conj history (render head history)))
        )
    )
  )


(def linear-dialog
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
   {:step     :use-clci-actions-label
    :element  :text
    :text     "Would you like to add aliases to your product to use the following Actions?"}
   {:step     :select-action-aliases
    :element  :choose
    :limit    2
    :options  [{:name "kondo" :key :kondo} {:name "clj-format" :key :clj-format}]}
   {:step     :select-template-label
    :element  :text
    :text     "Please select a template for your new product:"}
   {:step     :select-template
    :element  :choose
    :filter   (fn [history options] true)
    :options  [{:name "Clojure" :key :clojure} 
               {:name "Babashka" :key :babashka}
               {:name "ClojureScript" :key :clojurescript}
               {:name "Nbb" :key :nbb}]}])



(run-linear-dialog linear-dialog)

(defn create-empty-library
  ""
  [root use-kondo? use-cljformat?]
  )

(defn add-product
  "Add a new product to the repo."
  [{:keys [root key kind initial-version no-release?] :or {no-release? false initial-version "0.0.0"}}]
  ; (if-not (and
  ;           (s/valid? :clci.repo.product/root root)
  ;           (s/valid? :clci.repo.product/key key)
  ;           (s/valid? :clci.repo.product/version initial-version)
  ;           (s/valid? :clci.repo.product/root root))
  (let [current-repo  (repo/read-repo)
        new-product   {:root        root
                       :key         key
                       :type        kind
                       :version     initial-version
                       :no-release? no-release?}
        repo'         (update-in current-repo [:products] (fn [old] (conj old new-product)))]
    (when (= :clojure.spec.alpha/invalid (s/conform :clci.repo/product new-product))
      (throw (ex-info "The product specification does not conform to spec!" {})))
    ;(fs/create-dir root)
    ;(repo/write-repo! repo')
    )
  )

(empty? '())
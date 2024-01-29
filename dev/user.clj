(ns user
  ""
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [sh shell]]
    [bblgum.core :refer [gum]]
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

;; (defn installed?
;;   "Test if gum is installed on the machine."
;;   []
;;   (try
;;     (shell {:out :string :err :string} "gum --version")
;;     true
;;     (catch Exception e
;;       false)))


(defn- shell-exit-with-error?
  "Test if the given `exit-code` indicates a shell process did exit with an error."
  [exit-code]
  (not= exit-code 0))


;; (defn- yield-failure
;;   "Yield an error map for a dialog `step` when a `failure?` occured."
;;   [failure? step]
;;   {:step      step
;;    :failure?  failure?})


;; (defn- start-in-new-line
;;   "Begin terminal output on a new line."
;;   []
;;   (print "\n"))


;; (defn- print-welcome
;;   "Print the clci assistant welcome message on the terminal."
;;   []
;;   (print "Welcome to the" (yellow "clci") "setup assistant.\n"))


;; (defmulti render (fn [{:keys [element]} _] element))


;; (defmethod render :text [elem _]
;;   (if (:linebreak? elem)
;;     (print (str (:text elem) "\n"))
;;     (print (:text elem)))
;;   {:step (:step elem)})


;; (defmethod render :choose [elem history]
;;   (let [filter-fn               (get elem :filter (fn [_ options] options))
;;         options                 (filter-fn history (:options elem))
;;         limit                   (get elem :limit 1)
;;         step                    (:step elem)
;;         {:keys [status result]} (gum :choose (mapv :name options) :limit limit)]
;;     (if (= status 0)
;;       {:step step
;;        :selected-options (filter (fn [opt] (in? result (:name opt))) options)}
;;       {:step      step
;;        :failure?  true})))


;; (defmethod render :input [elem _]
;;   (let [placeholder     (get elem :placeholder "...")
;;         step            (:step elem)
;;         {:keys [status result]} (gum :input :placeholder placeholder)]
;;     (if (= status 0)
;;       {:step step
;;        :input (first result)}
;;       {:step      step
;;        :failure?  true})))


;; (defmethod render :wait [elem _]
;;   (-> (shell {:out :string} (format "sleep %s" (get elem :seconds 0)))
;;       :exit
;;       (shell-exit-with-error?)
;;       (yield-failure (:step elem))))


;; (defn run-linear-dialog
;;   ""
;;   [dialog]
;;   (start-in-new-line)
;;   (print-welcome)
;;   (loop [head     (first dialog)
;;          tail     (rest dialog)
;;          history  []]
;;     (cond
;;       ;; no more items left
;;       (nil? head)
;;       history
;;       ;; error on the previous item
;;       (-> history last :failure?)
;;       (ex-info "Unable to execute full dialog!" {:history history})
;;       ;; otherwise continue with the next element
;;       :else
;;       (recur (first tail) (rest tail) (conj history (render head history))))))


;; (defn- find-step
;;   "Find the step of the given `history` identified by the `step` key."
;;   [history step]
;;   (->> history
;;        (filter (fn [el] (= (:step el) step)))
;;        first))


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
    :filter   (fn [history options]
                options)
    :options  [{:name "Clojure" :key :clojure}
               {:name "Babashka" :key :babashka}
               {:name "ClojureScript" :key :clojurescript}
               {:name "Nbb" :key :nbb}]}])



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


;; Setup


;; (defn get-linux-os-details
;;   ""
;;   []
;;   (let [os-release (-> (shell {:out :string} "cat /etc/os-release")
;;                        :out
;;                        str/split-lines)
;;         os-name    (-> os-release
;;                        first
;;                        (str/split #"=")
;;                        second
;;                        (str/split #"\"")
;;                        second)
;;         kernel-release (-> (shell {:out :string} "uname -r") :out str/trim)]
;;     {:name    os-name
;;      :kernel  kernel-release}))


;; (defmulti get-os-details (fn [os-type] os-type))
;; (defmethod get-os-details "Linux" [_] (get-linux-os-details))
;; (defmethod get-os-details :default [_] nil)


;; (defn get-system-information
;;   []
;;   (let [os-type     (System/getProperty "os.name")
;;         os-details  (get-os-details os-type)]

;;     (merge
;;       {:os-type os-type}
;;       {:architecture (System/getProperty "os.arch")}
;;       os-details)))


;; (get-system-information)


;; (def required-binaries-unix
;;   "A list of all binaries required to setup clci.
;;    All items have the form [binary version-fn] where the later
;;    is a function that takes the string output of '<binary> --version'
;;    and parses the actual version from the installed binary and
;;    returns it as string."
;;   [["gum" (fn [s] (-> s (str/split #" ") (get 2)))]
;;    ["git" (fn [s] (-> s (str/split #" ") (get 2)))]])


;; (defn unix-binary-installed?
;;   "Test if the required binary is installed on the machine.
;;    Takes the `binary` as string argument and a function that takes
;;    the output of the '<binary> --version' command and returns the
;;    actual version of the installed binart. It can either be the
;;    command entered in the terminal (i.e. 'ls') or the full path
;;    to the binary (i.e. '/usr/bin/local/awesometool').
;;    The test is performed by using the Unix convention that an
;;    application will print its version when executed with the
;;    '--version' option. If the binary does not exist, the execution
;;    will yield a non zero error code."
;;   [[binary v-f]]
;;   (try
;;     [binary
;;      (-> (shell {:out :string :err :string} (format "%s --version" binary))
;;          :out
;;          v-f)
;;      ::ok]
;;     (catch Exception e
;;       [binary nil ::missing])))

;; ;;
;; ;; Returns nil when check was successful, `::failure` otherwise
;; (defmulti pre-setup-check (fn [os & _] os))


;; (defmethod pre-setup-check "Linux" [_ & {:keys [silent?] :or {silent? false}}]
;;   (println silent?)
;;   (let [installed-binaries      (mapv unix-binary-installed? required-binaries-unix)
;;         all-binaries-installed? (every? (fn [[_ _ status]] (= status ::ok)) installed-binaries)]
;;     (when-not silent?
;;       (doseq [[binary version status] installed-binaries]
;;         (println (str
;;                    (blue binary)
;;                    (when version (str "(" (yellow version) ")"))
;;                    " is "
;;                    (when (= status ::missing) (red "not"))
;;                    "installed"
;;                    (if (= status ::ok) (green "\u2713") (red "\u2A2F"))))))
;;     (when-not all-binaries-installed?
;;       ::failure)))


;; (def repository-setup-dialog
;;   ""
;;   [{:step       :welcome-msg
;;     :element    :text
;;     :text       (str/join
;;                   "\n"
;;                   ["You are about to setup the current repository using clci."
;;                    "This assistant will guide you through the steps."])
;;     :linebreak? true}
;;    {:step     :wait-before-start
;;     :element  :wait
;;     :seconds  1}
;;    {:step     :scm-provider-question-label
;;     :element  :text
;;     :text     "Which SCM provider would you like to use?"}
;;    {:step     :select-scm-provider
;;     :element  :choose
;;     :options  [{:name "Github" :key :github}]}
;;    {:step     :scm-repository-name-question-label
;;     :element  :text
;;     :text     "What name has your repository at the SCM?"}
;;    {:step     :select-scm-repository-name
;;     :element  :input
;;     :placeholder "Repository Name"}
;;    {:step     :scm-repository-owner-question-label
;;     :element  :text
;;     :text     "Who is the owner of your repository at the SCM?"}
;;    {:step     :select-scm-repository-owner
;;     :element  :input
;;     :placeholder "Repository Owner"}])


;; (run-linear-dialog repository-setup-dialog)


;; (defn setup-repository-assistant
;;   ""
;;   []
;;   (let [{:keys [os-type]} (get-system-information)]
;;     (print-welcome)
;;     (cond
;;       ;;
;;       (not (git/is-repository?))
;;       (do
;;         (println "The current directory is " (red "not") "a git repository!")
;;         (println "The setup assistant must be run at the root valid git repository."))
;;       ;; Validate the OS is supported by the assistant
;;       (not= "Linux" os-type)
;;       (println "Your OS is not supported by the setup assistant.")
;;       ;; Validate the preflight check is good
;;       (= ::failure (pre-setup-check os-type {}))
;;       (do
;;         (println (red "Unable to run the setup assistant."))
;;         (println (blue "At least one required binary is missing on your system."))
;;         (println (blue "Please install all required dependencies and try again.")))
;;       :else
;;       (let [data            (run-linear-dialog repository-setup-dialog)
;;             scm             :git
;;             scm-provider    (-> data (find-step :select-scm-provider) :selected-options first)
;;             scm-repo-name   (-> data (find-step :select-scm-repository-name) :input)
;;             scm-repo-owner  (-> data (find-step :select-scm-repository-owner) :input)]
;;         (println scm scm-provider scm-repo-name scm-repo-owner)
;;         (repo/repo-base {:scm scm
;;                          :scm-provider (:key scm-provider)
;;                          :scm-repo-name scm-repo-name
;;                          :scm-repo-owner scm-repo-owner})
;;         ))))


;; (setup-repository-assistant)

;; (pre-setup-check "Linux")


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

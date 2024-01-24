(ns clci.assistant.setup
  "This module defines an assistant to setup clci."
  (:require
    [babashka.process :refer [shell]]
    [clci.assistant.dialog :as dialog]
    [clci.git :as git]
    [clci.repo :as repo]
    [clci.term :refer [blue red green yellow magenta]]
    [clojure.string :as str]))


(defn- print-welcome
  "Print the clci assistant welcome message on the terminal."
  []
  (print "Welcome to the" (yellow "clci") "setup assistant.\n"))


(defn get-linux-os-details
  "Get details about a linux operating system and return it as map.
   This includes the `:name` of the linux distribution and the
   `:kernel-release`."
  []
  (let [os-release (-> (shell {:out :string} "cat /etc/os-release")
                       :out
                       str/split-lines)
        os-name    (-> os-release
                       first
                       (str/split #"=")
                       second
                       (str/split #"\"")
                       second)
        kernel-release (-> (shell {:out :string} "uname -r") :out str/trim)]
    {:name    os-name
     :kernel  kernel-release}))


(defmulti get-os-details (fn [os-type] os-type))
(defmethod get-os-details "Linux" [_] (get-linux-os-details))
(defmethod get-os-details :default [_] nil)


(defn get-system-information
  "Get basic information about the current system and return it as map.
   This includes the `:os-type` (i.e. \"Linux\") and the `:architecture`."
  []
  (let [os-type     (System/getProperty "os.name")
        os-details  (get-os-details os-type)]

    (merge
      {:os-type os-type}
      {:architecture (System/getProperty "os.arch")}
      os-details)))


(def required-binaries-unix
  "A list of all binaries required to setup clci.
   All items have the form [binary version-fn] where the later
   is a function that takes the string output of '<binary> --version'
   and parses the actual version from the installed binary and
   returns it as string."
  [["gum" (fn [s] (-> s (str/split #" ") (get 2)))]
   ["git" (fn [s] (-> s (str/split #" ") (get 2) (str/trim)))]])


(defn unix-binary-installed?
  "Test if the required binary is installed on the machine.
   Takes the `binary` as string argument and a function that takes
   the output of the '<binary> --version' command and returns the
   actual version of the installed binart. It can either be the
   command entered in the terminal (i.e. 'ls') or the full path
   to the binary (i.e. '/usr/bin/local/awesometool').
   The test is performed by using the Unix convention that an
   application will print its version when executed with the
   '--version' option. If the binary does not exist, the execution
   will yield a non zero error code."
  [[binary v-f]]
  (try
    [binary
     (-> (shell {:out :string :err :string} (format "%s --version" binary))
         :out
         v-f)
     ::ok]
    (catch Exception _
      [binary nil ::missing])))



;; Run several checks before clci can be setup.
;; Returns nil when check was successful, `::failure` otherwise
(defmulti pre-setup-check (fn [os & _] os))


;; Execute the pre setup checks specific for systems running linux.
(defmethod pre-setup-check "Linux" [_ & {:keys [silent?] :or {silent? false}}]
  (let [installed-binaries      (mapv unix-binary-installed? required-binaries-unix)
        all-binaries-installed? (every? (fn [[_ _ status]] (= status ::ok)) installed-binaries)]
    (when-not silent?
      (doseq [[binary version status] installed-binaries]
        (println (str
                   (blue binary)
                   (when version (str " (" (yellow version) ")"))
                   " is "
                   (when (= status ::missing) (red "not"))
                   "installed "
                   (if (= status ::ok) (green "\u2713") (red "\u2A2F"))))))
    (when-not all-binaries-installed?
      ::failure)))



(def repository-setup-dialog
  "Dialog used to assist a user to setup a newv repository with clci."
  [{:step       :welcome-msg
    :element    :text
    :text       (str/join
                  "\n"
                  ["You are about to setup the current repository using clci."
                   "This assistant will guide you through the steps."])
    :linebreak? true}
   {:step     :wait-before-start
    :element  :wait
    :seconds  1}
   {:step     :scm-provider-question-label
    :element  :text
    :text     "Which SCM provider would you like to use?"}
   {:step     :select-scm-provider
    :element  :choose
    :options  [{:name "Github" :key :github}]}
   {:step     :scm-repository-name-question-label
    :element  :text
    :text     "What name has your repository at the SCM?"}
   {:step     :select-scm-repository-name
    :element  :input
    :placeholder "Repository Name"}
   {:step     :scm-repository-owner-question-label
    :element  :text
    :text     "Who is the owner of your repository at the SCM?"}
   {:step     :select-scm-repository-owner
    :element  :input
    :placeholder "Repository Owner"}
   {:step     :summary
    :element  :text
    :format   (fn [_ history]
                (str/join "\n"
                          [(format "%s: %s" (blue "SCM Provider") (-> history (dialog/find-step :select-scm-provider) :selected-options first :name magenta))
                           (format "%s: %s" (blue "Repository Name") (-> history (dialog/find-step :select-scm-repository-name) :input green))
                           (format "%s: %s" (blue "Repository Owner") (-> history (dialog/find-step :select-scm-repository-owner) :input yellow))]))}
   {:step     :confirm-label
    :element  :text
    :text     "Are your declarations correct?"}
   {:step     :confirmation
    :element  :confirm
    :options  [{:name "Yes" :key :yes} {:name "No" :key :no}]}])



(defn run-setup-assistant
  "Run the setup assistant.
   
   The setup assistant helps a user to setup a repository managed using clci.
   
   Before setting up the actual repository several checks are performed.
   After passing all checks the setup dialog is started and the repository
   is setup to use clci according to the user input."
  [_]
  (let [{:keys [os-type]} (get-system-information)]
    (print-welcome)
    (cond
      ;;
      (not (git/is-repository?))
      (do
        (println "The current directory is " (red "not") "a git repository!")
        (println "The setup assistant must be run at the root valid git repository."))
      ;; Validate the OS is supported by the assistant
      (not= "Linux" os-type)
      (println "Your OS is not supported by the setup assistant.")
      ;; Validate the preflight check is good
      (= ::failure (pre-setup-check os-type {}))
      (do
        (print (red "Unable to run the setup assistant.\n"))
        (print (blue "At least one required binary is missing on your system.\n"))
        (print (blue "Please install all required dependencies and try again.\n"))
        (flush))
      :else
      (let [user-input      (dialog/run-linear-dialog repository-setup-dialog)
            scm             :git
            scm-provider    (-> user-input (dialog/find-step :select-scm-provider) :selected-options first)
            scm-repo-name   (-> user-input (dialog/find-step :select-scm-repository-name) :input)
            scm-repo-owner  (-> user-input (dialog/find-step :select-scm-repository-owner) :input)]
        (when-not (-> user-input (dialog/find-step :confirmation) :input)
          (System/exit 1))
        (-> {:scm scm
             :scm-provider (:key scm-provider)
             :scm-repo-name scm-repo-name
             :scm-repo-owner scm-repo-owner}
            (repo/repo-base)
            (repo/write-repo!))))))

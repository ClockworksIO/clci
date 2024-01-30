(ns clci.assistant.dialog
  ""
  (:require
    [babashka.process :refer [shell]]
    [bblgum.core :refer [gum]]
    [clci.util.core :refer [in?]]
    [clojure.string :as str]))



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


(defmulti render
  ""
  (fn [{:keys [element]} _] element))


(defmethod render :text [elem history]
  (let [linebreak?  (get elem :linebreak? true)
        text        (if-let [fmt-fn (:format elem)]
                      (fmt-fn (:text elem) history)
                      (:text elem))]
    (if linebreak?
      (print (str text "\n"))
      (print text))
    {:step (:step elem)}))


(defmethod render :choose [elem history]
  (let [filter-fn               (get elem :filter (fn [_ options] options))
        options                 (filter-fn history (:options elem))
        limit                   (get elem :limit 1)
        step                    (:step elem)
        {:keys [status result]} (gum :choose (mapv :name options) :limit limit)]
    (if (= status 0)
      {:step step
       :selected-options (filter (fn [opt] (in? result (:name opt))) options)}
      {:step      step
       :failure?  true})))


(defmethod render :input [elem _]
  (let [placeholder     (get elem :placeholder "...")
        step            (:step elem)
        {:keys [status result]} (gum :input :placeholder placeholder)]
    (if (= status 0)
      {:step step
       :input (first result)}
      {:step      step
       :failure?  true})))


(defmethod render :wait [elem _]
  (-> (shell {:out :string} (format "sleep %s" (get elem :seconds 0)))
      :exit
      (shell-exit-with-error?)
      (yield-failure (:step elem))))


(defn find-step
  "Find the step of the given `history` identified by the `step` key."
  [history step]
  (->> history
       (filter (fn [el] (= (:step el) step)))
       first))



(defn run-linear-dialog
  ""
  [dialog]
  (start-in-new-line)
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
      (recur (first tail) (rest tail) (conj history (render head history))))))



(def repository-setup-dialog
  ""
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
                          [(format "SCM Provider: %s" (-> history (find-step :select-scm-provider) :selected-options first :name))
                           (format "Repository Name: %s" (-> history (find-step :select-scm-repository-name) :input))
                           (format "Repository Owner: %s" (-> history (find-step :select-scm-repository-owner) :input))]))}
   {:step     :confirm-label
    :element  :text
    :text     "Please confirm your declarations:"}
   {:step     :confirm
    :element  :choose
    :options  [{:name "Yes" :key :yes} {:name "No" :key :no}]}])


;; (run-linear-dialog repository-setup-dialog)


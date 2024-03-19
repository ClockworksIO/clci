(ns clci.assistant.dialog
  "This module provides the means to define and execute dialogs
   in a terminal for interactive user input.
   The module uses [gum](https://github.com/charmbracelet/gum) and simple
   build-in mechanisms to display information and read user input."
  (:require
    [babashka.process :refer [shell]]
    [bblgum.core :refer [gum]]
    [clci.util.core :refer [in?]]))



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
  "Render an item from a dialog declaration.
   
   This multimethod takes an element from a dialog declaration
   and renders the corresponding input or output element in the
   terminal.
   
   All elements receive a map of keyword indexed arguments to 
   control the rendering of the element and the history of the
   previous user input. The later can be used to render contextual
   information.
   
   Each method returns a map with the mandatory `:step` key holding the
   key of the dialog step, the optional `:failure?` key with a boolean
   value indicating if a failure occured while rendering the dialog step
   and a set of dialog element specific keys.
   ```"
  (fn [{:keys [element]} _] element))


;; Render a simple Text element
;; A Text element is a simple print statement on the active terminal.
;; Takes the mandatory keyword arg `:text` with the actual text
;; Also takes the optional arguments:
;; | key                 | description                         |
;; | --------------------|-------------------------------------|
;; | `:linebreak?`       | Indicator if to add a linebreak after the text (defaults as true)
;; | `:format`           | Function `(string, history) -> string` to format the text using data from the input history
(defmethod render :text [elem history]
  (let [linebreak?  (get elem :linebreak? true)
        text        (if-let [fmt-fn (:format elem)]
                      (fmt-fn (:text elem) history)
                      (:text elem))]
    (if linebreak?
      (print (str text "\n"))
      (print text))
    (flush)
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


(defmethod render :confirm [elem _]
  (let [step (:step elem)
        {:keys [status result]} (gum :confirm :as :bool :selected.border "normal")]
    (if (= status 0)
      {:step step
       :input result}
      {:step      step
       :failure?  true})))


(defmethod render :wait [elem _]
  (-> (shell {:out :string} (format "sleep %s" (get elem :seconds 0)))
      :exit
      (shell-exit-with-error?)
      (yield-failure (:step elem))))


(defn find-step
  "Find a step in the given `history` identified by the `step` key."
  [history step]
  (->> history
       (filter (fn [el] (= (:step el) step)))
       first))



(defn run-linear-dialog
  "Run a dialog in liear order based on the diven `dialog` declaration.
   Stops execution if an error occurs. Returns the history of the dialog."
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

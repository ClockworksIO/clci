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
        post-tf         (get elem :post-fn identity)
        {:keys [status result]} (gum :input :placeholder placeholder)]
    (if (= status 0)
      {:step      step
       :input     (post-tf (first result))
       :raw-input (first result)}
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


(defn find-selected-option
  "Find and return the user selection from a dialog Choose step
   with a single select.
   Takes the dialogs `history` and the key of the `step`."
  [history step]
  (-> (find-step history step)
      :selected-options
      first))


(defn find-selected-options
  "Find and return the user selections from a dialog Choose step.
   Takes the dialogs `history` and the key of the `step`."
  [history step]
  (-> (find-step history step)
      :selected-options))


(defn find-input
  "Find and return the user input from a dialog Input step.
   Takes the dialogs `history` and the key of the `step`."
  [history step]
  (-> (find-step history step)
      :input))


(defn- not-empty-seq?
  "Predicate to test if the given `sq` is a non-empty sequence."
  [sq]
  (and (seq? sq) (seq sq)))


(defn- find-step-rec
  "Implementation of `find-in-step`.
   Recursively walks the given `step-data` based on the `arg-path` and
   returns the element specified by the path if it exists."
  [step-data arg-path]
  (cond
    ;;
    (empty? arg-path)
    step-data
    ;;
    (and (not-empty-seq? step-data) (fn? (first arg-path)))
    (find-step-rec ((first arg-path) step-data) (rest arg-path))
    ;;
    (seq? step-data)
    (find-step-rec (nth step-data (first arg-path) nil) (rest arg-path))
    ;;
    (coll? step-data)
    (find-step-rec (get step-data (first arg-path)) (rest arg-path))
    ;;
    :else step-data))


(defn find-in-step
  "Get a specific nested value from a dialog input step.
   Takes the dialogs `history`, the key of the `step` and a path
   of nested qualifiers `arg` and returns the value at the given path.
   Similar to `(get-in col path)`.
   See example below."
  [history step & args]
  (let [step-data (find-step history step)]
    (find-step-rec step-data args)))


(comment
  ;; Example on how the `find-in-step` 
  (def history
    [{:step :welcome-msg}
     {:step :wait-before-start, :failure? false}
     {:step :product-type-question-label}
     {:step :select-product-type, :selected-options '({:name "App", :key :app})}
     {:step :select-template-label}
     {:step :select-template, :selected-options '({:name "Clojure", :key :clojure})}
     {:step :product-name-label}
     {:step :product-name, :input "aProductAwesome"} {:step :product-nondefault-root-label}
     {:step :product-nondefault-root-select, :selected-options '({:name "Yes", :key :yes})}
     {:step :product-root-label}
     {:step :product-root, :input "./a-path"}
     {:step :use-clci-actions-label}
     {:step :select-action-aliases, :selected-options ()}])

    (find-in-step history :product-name)
    (find-in-step history :product-name :input)
    (find-in-step history :product-nondefault-root-select :selected-options)
    (find-in-step history :product-nondefault-root-select :selected-options first)
    (find-in-step history :product-nondefault-root-select :selected-options first :name)
    (find-in-step history :product-nondefault-root-select :selected-options 0)
    (find-in-step history :product-nondefault-root-select :selected-options 0 :name)
)


(defn- at-least
  "Returns `value` unless the value is smaller than `min`, then
   min is returned"
  [value min]
  (if (< value min)
    min
    value))


(defn run-linear-dialog
  "Run a dialog in linear order based on the given `dialog` declaration.
   Stops execution if an error occurs. Returns the history of the dialog."
  [dialog]
  (let [skip-fn     (fn [step] (or (get step :skip?) (constantly 0)))
        skip-count  (fn [step history] (at-least ((skip-fn step) step history) 0))
        skip?       (fn [step history] (> (skip-count step history) 0))]
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
        ;; step should be skipped?
        (skip? head history)
        (recur (first (drop (skip-count head history) tail)) (rest (drop (skip-count head history) tail)) history)
        ;; otherwise continue with the next element
        :else
        (recur (first tail) (rest tail) (conj history (render head history)))))))


;; TODO: the skip mechanism requires some explanation

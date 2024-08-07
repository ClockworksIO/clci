(ns clci.util.dev
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :refer [instrument unstrument]]))


;;
;; ToDo Macro ;;;;
;;

;; The ToDo macro provides a simple but consistent way to work with ToDo items
;; directly from the repl.
;; The macro allows a Developer to declare and describe simple todo items directly in
;; a clojure source file. These todos can be used to outline and plan different small
;; steps and tasks that are required to implement the issue a developer is currently
;; resolving.
;; It is not intended as a replacement for an issue/project management tool like
;; Github or Jira.

(s/def ::desc string?)
(s/def ::scopes (s/coll-of symbol?))
(s/def ::issue (s/or :str string? :kw keyword?))


(s/def ::todo-meta
  (s/keys
    :req-un [::desc]
    :opt-un [::scopes ::issue]))


(s/def ::todo-status #{:todo :done})
(s/def ::todo-title string?)
(s/def ::todo-item (s/tuple ::todo-status ::todo-title))
(s/def ::todo-group-key keyword?)
(s/def ::todo-group-name string?)


(s/def ::todo-group
  (s/and
    vector?
    (s/cat :key ::todo-group-key :name ::todo-group-name :items (s/+ ::todo-item))))


(s/def ::complex-todo
  (s/and
    seq?
    (s/cat :meta ::todo-meta :groups (s/+ ::todo-group))))


(s/def ::simple-todo (s/cat :text string?))


(s/fdef todo-impl
        :args (s/cat :body
                     (s/or
                       :simple  ::simple-todo
                       :complex ::complex-todo))
        :ret  nil?)


(defn todo-impl
  "Internal impl. of the ToDo macro.
   Does nothing but is required to run a compile/evaluation time check on the arguments
   passed to the `ToDo` macro."
  [_]
  nil)


;; Need to instrument the function to enable spec checks on the todo macro at evaluation time.
(instrument `todo-impl)


(defmacro ToDo
  "A simple macro to make ToDo annotations in the code. Body is not evaluated."
  [& body]
  (todo-impl body))


(comment
  ;; Simple ToDo example with a single text item
  (ToDo "this is a todo" )
 
  ;; Complex todo example
  ;; Defines a ToDo list with a scope pointing to an issue and
  ;; setting a scope to a specific module.
  ;; Also structures the todo items into groups
  (ToDo
   {:desc "dd" 
    :issue "clci-123"
    :scopes [clci.util.dev]}
   [:group-1 "some foo" 
    [:todo "faxe faxe mache"]
    [:done "faxe faxe mache"]]
   [:other "some foo" 
    [:todo "blah"]])

  ;; This would be invalid
  (ToDo :f) 
)


;;
;; Macros for documentation ;;;;
;;

;; The following macros help to document code and may be used to generate documentation
;; in the future.

(defmacro document-spec
  "Document a clojure spec.
   Takes a metadata map `mt`, the keyword ident of the `spec` and
   an arbitrary form to document the spec."
  [mt spec & body])


(defmacro document
  "Takes a reference `rf` and a map with meta data `mt`. 
   Ignores the `body`, yields nil.
   This macro is reserved for generating documentation from the code in the future.
   Right now it behaves exactly like the clojure core `comment` macro."
  [mt rf & body])

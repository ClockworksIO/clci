(ns clci.util.error
  "Error handling functions.

  This module holds a set of functions for nicer clojure style error handling.
  Based on https://adambard.com/blog/acceptable-error-handling-in-clojure/")


;; A Failure is a container that holds the cause, trace and location of a
;; runtime error. A Failure is an error that can be recovered from.
(defrecord Failure
  [cause via trace])


(defn failure?
  "Test if result is an error.

  Takes a parameter vector of form [value type] that either holds an error
  or a valid result. Returns true if the input holds an error."
  [v]
  (if (instance? Failure v)
    true
    false))


(defn fail!
  "Create a Failure."
  [cause & {:keys [via trace] :or {via nil trace nil}}]
  (Failure. cause via trace))


(defn apply-or-fail
  "Try to apply the function `f` on value `val`. If an exception is thrown,
  return a `Failure` with the cause and trace of the raised exception. Otherwise
  return the result of the function applied to the value."
  [f val]
  (if-not (failure? val)
    (try
      (f val)
      (catch Exception e
        (let [tm (Throwable->map e)]
          (fail! (tm :cause) :via (tm :via) :trace (tm :trace)))))
    val))


(defmacro fail->
  "Like the common threading macro `->` but wraps every function call in an
  `apply-or-fail`. If a Failure occurs, the remaining functions are not applied,
  instead the Failure gets passed on and is returned."
  [val & fns]
  (let [fns (for [f fns] `(apply-or-fail ~f))]
    `(-> ~val
         ~@fns)))


(defmacro fail->>
  "Like the common threading macro `->>` but wraps every function call in an
  `apply-or-fail`. If a Failure occurs, the remaining functions are not applied,
  instead the Failure gets passed on and is returned."
  [val & fns]
  (let [fns (for [f fns] `(apply-or-fail ~f))]
    `(->> ~val
          ~@fns)))

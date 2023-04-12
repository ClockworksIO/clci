(ns user
  ""
  (:require
    [babashka.fs :as fs]
    [clci.conventional-commit :refer [valid-commit-msg?]]
    [clci.term :as c]
    [clci.util :refer [slurp-edn]]
    [clci.workflow :as wfl]
    [cljc.java-time.local-date :as ld]
    [clojure.core.async :as a]
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as s]))


;; (require '[clci.actions])

(def ^:private lock (Object.))


(defn- serialized-require
  [& args]
  (locking lock
    (apply require args)))


(defn req-resolve
  [sym]
  (if (qualified-symbol? sym)
    (or (resolve sym)
        (do (-> sym namespace symbol serialized-require)
            (resolve sym)))
    (throw (IllegalArgumentException. (str "Not a qualified symbol: " sym)))))


(def foo
  (-> (wfl/get-workflows)
      first
      :jobs
      first
      :action


      ;; type
      ;; symbol
      ;; requiring-resolve
      ;; deref
      ;; resolve
      ))


foo

(symbol (namespace foo))

@(req-resolve foo)


;; @(req-resolve 'clci.actions/create-random-integer-action)

;; (ns-resolve (symbol (namespace foo)) (symbol (name foo)))


;; (deref (resolve 'clci.actions/create-random-integer-action))


;; ;; (var-get (requiring-resolve 'clci.actions/create-random-integer-action))


(re-matches #"!job.[a-zA-Z0-9\-]+" (or (namespace :error) ""))

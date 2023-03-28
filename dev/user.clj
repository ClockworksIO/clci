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


(symbol (namespace foo))

(ns-resolve (symbol (namespace foo)) (symbol (name foo)))


(deref (resolve 'clci.actions/create-random-integer-action))


;; (var-get (requiring-resolve 'clci.actions/create-random-integer-action))


(re-matches #"\[^(.*)\/([^\/\"]*)\]" "[/foo/bar]")

(re-matches #"\[(\"((.*)\/([^\/\"]*))+\")+\]" "[\"foo/bar\"]")


(-> (re-matches #"\[((\"((.*)\/([^\/\"]*))+\"),?)+\]" "[\"src/\"]")
    second
    (str/split #","))


(-> (re-matches #"\[((((.*)\/([^\/\"]*))+),?)+\]" "[src/,foo/]")
    second
    (str/split #","))


(re-matches #"\[((\"((.*)\/([^\/\"]*))+\"),?)+\]" "[\"foo/bar\",\"src\"]")

(re-matches #"^(.*)\/([^\/\"]*)" "/foo/bar")

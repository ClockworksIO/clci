(ns clci.test-utils
  (:require
    ;; [clci.release :as rel]
    ;; [clci.repo :as rp]
    [clojure.spec.alpha :as s]
    ;; [clojure.spec.gen.alpha :as gen]
    ;; [clojure.test :refer [deftest testing is]]
    [miner.strgen :as sg]))



(def commit-hash-re
  "Regex to describe the hash of a git commit."
  #"[0-9a-f]{5,40}")


;; Spec for a git commit hash.
;; Can be used to generate commit hashes i.e. for generative testing.
(s/def ::commit-hash
  (s/spec
    (s/and string? #(re-matches commit-hash-re %))
    :gen #(sg/string-generator commit-hash-re)))


(def rfc3339-datetime-re
  "Regex for rfc33 datetime strings."
  #"(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2}(\.\d+)?(Z|([\+-]\d{2}:\d{2}))?)")


;; Spec for git commit dates following the rfc33 specification
;; Can be used to generate commit dates i.e. for generative testing.
(s/def ::commit-date
  (s/spec (s/and string? #(re-matches rfc3339-datetime-re %))
          :gen #(sg/string-generator rfc3339-datetime-re)))


(def commit-author-re
  "Regex for a git commit author. Assumed to be roughly an email address."
  #"[0-9a-fA-Z]{5,35}@[0-9a-fA-Z]{5,35}\.[a-z]{2,5}")


;; Spec for git commit authors.
;; Can be used to generate commit authors i.e. for generative testing.
(s/def ::commit-author
  (s/spec (s/and string? #(re-matches commit-author-re %))
          :gen #(sg/string-generator commit-author-re)))



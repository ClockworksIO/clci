(ns user
  ""
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [sh shell]]
    [bblgum.core :refer [gum]]
    [clci.assistant.dialog :as dialog]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.release :as rel]
    [clci.repo :as repo]
    [clci.term :refer [blue red green yellow grey white cyan]]
    [clci.util.core :refer [in?]]
    [clojure.core :as c]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))


(ns clci.main
  "This module exposes a pod interface for clci to be used from babashka."
  (:gen-class)
  (:require
    [clci.conventional-commit :as cc]
    [pod-racer.core :as pod]))


(def pod-config
  {:pod/namespaces
   [{:pod/ns "clci.pod"
     :pod/vars [{:var/name "valid-commit-msg?"
                 :var/fn cc/valid-commit-msg?}]}]})


(defn -main
  [& _args]
  (pod/launch pod-config))

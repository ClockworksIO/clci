(ns clci.main
  ""
  (:gen-class)
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]))


(def build-conf (-> (slurp "deps.edn") (edn/read-string)))
(def resources-dir (:resources-dir build-conf "resources"))

(def resource (slurp (str resources-dir "/" "example.txt")))


;; (def resource "s")

(defn -main
  [& _args]
  (println resource))

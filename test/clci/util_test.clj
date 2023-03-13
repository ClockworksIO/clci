(ns clci.util-test
  "This module provides tests for the `git` module."
  (:require
    [clci.util :as u]
    [clojure.test :refer [deftest testing is]]))



(deftest find-first-index
  (testing "Test utility function `find-first-index`."
    (is (= (u/find-first-index [1 2 3 4 5] #(= 4 %)) 3))
    (is (nil? (u/find-first-index [:a :b :c :d] #(= :e %))))
    (is (nil? (u/find-first-index [] even?)))
    (is (= (u/find-first-index '("hallo" "my" "test") #(= % "my")) 1))))

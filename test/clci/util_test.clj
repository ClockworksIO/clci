(ns clci.util-test
  "This module provides tests for the `git` module."
  (:require
    [clci.util.core :as u]
    [clojure.test :refer [deftest testing is]]))



(deftest find-first-index
  (testing "Test utility function `find-first-index`."
    (is (= (u/find-first-index [1 2 3 4 5] #(= 4 %)) 3))
    (is (nil? (u/find-first-index [:a :b :c :d] #(= :e %))))
    (is (nil? (u/find-first-index [] even?)))
    (is (= (u/find-first-index '("hallo" "my" "test") #(= % "my")) 1))))


(deftest in-and-notin
  (testing "Testing utility function `in?`."
    (is (u/in? [1 2 3 4] 4))
    (is (u/in? [:a :b 3 "d"] :b))
    (is (u/in? [:a :b 4 "c"] "c")))
  (testing "Testing utility function `not-in?`."
    (is (u/not-in? [1 2 3 4] 5))))


(deftest same-elements
  (testing "Testing utility function `same-elements?`."
    (is (u/same-elements? [1 2 3 4] [1 2 3 4]))
    (is (u/same-elements? [2 1 4 3] [1 2 3 4]))
    (is (u/same-elements? [1 2 2 4 5] [2 2 5 1 4]))
    (is (u/same-elements? [1 :a :a "c"] ["c" 1 :a :a]))
    (is (not (u/same-elements? [1 1 1 2 3 4] [1 2 3 4])))))

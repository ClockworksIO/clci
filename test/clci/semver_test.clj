(ns clci.semver-test
  "This module provides tests for the semantic versioning module."
  (:require
   [clci.semver :refer [valid-version-tag? major minor patch pre-release]]
   [clojure.test :refer [deftest testing is]]))

(deftest valid-tags
  (testing "Testing to validate tags that correctly follow the semver specs."
    (is (valid-version-tag? "0.2.1"))
    (is (valid-version-tag? "0.2.1-beta"))
    (is (valid-version-tag? "1.2.1-20221203"))
    (is (valid-version-tag? "1.2.1-20221203.1"))
    (is (valid-version-tag? "3.0.1-SNAPSHOT"))
    (is (valid-version-tag? "1.21.12-next"))))

(deftest in-valid-tags
  (testing "Testing to validate tags that NOT correctly follow the semver specs."
    (is (not (valid-version-tag? "0..1")))
    (is (not (valid-version-tag? "v0.2.1")))
    (is (not (valid-version-tag? "20221203")))
    (is (not (valid-version-tag? "bumblebee")))))

(deftest get-version-parts
  (testing "Testing utilities to get the version parts."
    (is (major "0.2.1") 0)
    (is (major "2.2.1") 2)
    (is (major "12.4.1-2.2beta") 12)
    (is (minor "2.2.1") 2)
    (is (minor "2.0.1-aplha") 0)
    (is (minor "2.21.1") 21)
    (is (patch "2.2.1") 1)
    (is (patch "2.2.77-3") 77)
    (is (patch "2.2.77-next1234") 77)
    (is (nil? (pre-release "2.2.77")))
    (is (pre-release "2.2.77-next") "next")
    (is (pre-release "2.2.77-next3.345") "next3.345")))



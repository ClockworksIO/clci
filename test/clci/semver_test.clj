(ns clci.semver-test
  "This module provides tests for the semantic versioning module."
  (:require
    [clci.semver :refer [valid-version-tag?]]
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

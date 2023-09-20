(ns clci.semver-test
  "This module provides tests for the semantic versioning module."
  (:require
    [clci.semver :refer [valid-semver? valid-version-tag? major minor patch pre-release newer?]]
    [clojure.test :refer [deftest testing is]]))


(deftest valid-versions
  (testing "Testing to validate version strings that correctly follow the semver specs."
    (is (valid-semver? "0.2.1"))
    (is (valid-semver? "0.2.1-beta"))
    (is (valid-semver? "1.2.1-20221203"))
    (is (valid-semver? "1.2.1-20221203.1"))
    (is (valid-semver? "3.0.1-SNAPSHOT"))
    (is (valid-semver? "1.21.12-next"))))


(deftest invalid-versions
  (testing "Testing to validate version strings that NOT correctly follow the semver specs."
    (is (not (valid-semver? "0..1")))
    (is (not (valid-semver? "v0.2.1")))
    (is (not (valid-semver? "20221203")))
    (is (not (valid-semver? "bumblebee")))))


(deftest valid-version-tags
  (testing "Testing to validate release tags using the `<release-prefix>-<semver>` format."
    (is (valid-version-tag? "release-0.2.1"))
    (is (valid-version-tag? "re-lea-se-0.2.1"))
    (is (valid-version-tag? "r3-le4-s3-0.2.1"))
    (is (valid-version-tag? "release-0.2.1-beta"))
    (is (valid-version-tag? "release-1.2.1-20221203"))
    (is (valid-version-tag? "release-1.2.1-20221203.1"))
    (is (valid-version-tag? "release-3.0.1-SNAPSHOT"))
    (is (valid-version-tag? "release-1.21.12-next"))))


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


(deftest compare-versions
  (testing "Test the `newer?` predicate."
    (is (newer? "1.2.3" "1.0.0"))
    (is (newer? "1.12.3" "3.4.1"))
    (is (newer? "0.1.0" "0.0.0"))
    (is (newer? "1.3.3" "1.3.2"))
    (is (newer? "1.5.3" "1.4.12"))
    (is (newer? "12.24.342" "1.0.0"))
    (is (newer? "51.0.0" "1.2.3"))
    (is (not (newer? "1.2.3" "1.2.5")))))

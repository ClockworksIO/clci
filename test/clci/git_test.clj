(ns clci.git-test
  "This module provides tests for the `git` module."
  (:require
    [clci.git :as git]
    [clci.test-data :as datasets]
    [clci.util.core :refer [same-elements?]]
    [clojure.test :refer [deftest testing is]]))


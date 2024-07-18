(ns tasks
  "This module defines various tasks that are available to work with the `clci` package.
  This includes perform tests and building the library."
  (:require
    [babashka.cli :as cli]
    [clci.carve :refer [carve-impl]]))

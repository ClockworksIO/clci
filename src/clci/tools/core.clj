(ns clci.tools.core
  "This modules exposes an api of several tools that can be used
  directly from a bb task in a project's codebase."
  (:require
    [clci.tools.antq :as aq]
    [clci.tools.carve :as carve]
    [clci.tools.cloverage :as cov]
    [clci.tools.format :as fmt]
    [clci.tools.ghooks :as gh]
    [clci.tools.linesofcode :as loc]
    [clci.tools.linter :as linter]
    [clci.tools.mkdocs :as mkdocs]))


(defn lint
  "Lint the code."
  [opts]
  (linter/lint opts))


(defn carve!
  "Find and optionally remove unused vars from the codebase."
  [opts]
  (carve/carve! opts))


(defn docs!
  "Build a documentation with mkdocs."
  [opts]
  (mkdocs/docs! opts))


(defn git-hooks
  "Run a git hook."
  [opts]
  (gh/hook opts))


(defn format!
  "Run the formatter on all Clojure files."
  [opts]
  (fmt/format! opts))


(defn lines-of-code
  "Get the lines of code of the project."
  [opts]
  (loc/lines-of-code opts))


(defn outdated-deps
  "Find outdated dependencies."
  [opts]
  (aq/find-outdated-dependencies opts))


(defn test-coverage
  "Get the test coverage of the project's code."
  [opts]
  (cov/cloverage opts))

(ns clci.changelog-test
  (:require
    [clci.changelog :refer [parser text->ast]]
    [clojure.test :refer [deftest testing is]]
    [instaparse.core :as insta]))


(defn parseable?
  "Helper function to test if the given `msg` (string) can be parsed with the given `parser`."
  [parser msg]
  (not (insta/failure? (insta/parse parser msg))))



(def changelog-text-1
  (str
    "# Changelog\n"
    "\n"
    "All notable changes to this project will be documented in this file.\n"
    "\n"
    "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
    "\n"
    "## [UNRELEASED]\n"
    "\n"
    "## [1.2.3] - 2023-04-23\n"
    "\n"
    "### Added\n"
    "\n"
    "some stuff to explain #99\n"
    "\n"
    "This commit adds a new mechanism how to use git hooks in projects using clci.\n"
    "\n"
    "A hook will send a trigger which is then processed using the clci workflow mechanism. See #clci-99.\n"
    "\n"
    "Some more paragraph.\n"
    "\n"
    "\n"))


(def changelog-text-2
  (str
    "# Changelog\n"
    "\n"
    "All notable changes to this project will be documented in this file.\n"
    "\n"
    "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
    "\n"
    "## [UNRELEASED]\n"
    "\n"
    "## [1.2.3] - 2023-04-23\n"
    "\n"
    "### Added\n"
    "\n"
    "- some stuff to explain #99\n"
    "- another awesome feature\n"
    "- more stuff #clci-99\n"
    "\n"
    "\n"))


(def changelog-text-3
  (str
    "# Changelog\n"
    "\n"
    "All notable changes to this project will be documented in this file.\n"
    "\n"
    "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
    "\n"
    "## [Unreleased]\n"
    "\n"
    "## [0.16.0] - 2023-04-14\n"
    "\n"
    "### Added\n"
    "\n"
    "- new awesome feature #clci-77\n"
    "- another feature\n"
    "- and again another awesome feature\n"
    "\n"))


(def changelog-text-4
  (str
    "# Changelog\n"
    "\n"
    "All notable changes to this project will be documented in this file.\n"
    "\n"
    "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
    "\n"
    "## [Unreleased]\n"
    "\n"
    "## [0.16.0] - 2023-04-14\n"
    "\n"
    "### Added\n"
    "\n"
    "- new awesome feature #clci-77\n"
    "- another feature\n"
    "- and again another awesome feature\n"
    "\n"
    "## [0.15.1] - 2023-03-31\n"
    "\n"
    "### Fixed\n"
    "\n"
    "- fixed the issue #22\n"
    "\n"
    "## [0.15.0] - 2023-03-11\n"
    "\n"
    "### Added\n"
    "\n"
    "awesome new feature see #23\n"
    "\n"
    "This is a long explanatory paragraph for this new feature referencing the issue #clci-23 because of reasons.\n"
    "\n"
    "## [0.12.0] - 2023-03-01\n"
    "\n"
    "### Added\n"
    "\n"
    "foo #21\n"
    "\n"
    "fasel blah long stuff #clci-21\n"
    "\n"
    "### Other\n"
    "\n"
    "- working on pipeline #13\n"
    "\n"))


(def changelog-text-5
  (str
    "# Changelog\n"
    "\n"
    "All notable changes to this project will be documented in this file.\n"
    "\n"
    "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), \n"
    "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
    "\n"
    "## [Unreleased]\n"
    "\n"
    "## [0.17.0] - 2023-04-14\n"
    "\n"
    "### Added\n"
    "\n"
    "new git hook mechanism #96\n"
    "\n"
    "This commit adds a new mechanism how to use git hooks in projects using clci. A hook will send a trigger which is then processed using the clci workflow mechanism.\n"
    "\n"
    "## [0.16.0] - 2023-04-11\n"
    "\n"
    "### Added\n"
    "\n"
    "- new awesome feature #clci-77\n"
    "\n"
    "## [0.15.1] - 2023-03-29\n"
    "\n"
    "### Fixed\n"
    "\n"
    "- fix invalid version parser #23\n"
    "\n"
    "### Other\n"
    "\n"
    "- update build pipeline for releases #11\n"
    "- cleanup dead code #12\n"
    "- refactor module structure #12\n"
    "\n"))


(def changelog-text-6
  (str
    "# Changelog\n"
    "\n"
    "All notable changes to this project will be documented in this file.\n"
    "\n"
    "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
    "\n"
    "## [Unreleased]\n"
    "\n"
    "## [0.16.0] - 2023-04-14\n"
    "\n"
    "### Added\n"
    "\n"
    "- new awesome feature #clci-77\n"
    "- another feature\n"
    "- and again another awesome feature\n"
    "\n"
    "### Fixed\n"
    "\n"
    "- problem with things\n"
    "- another problem with stuff #clci-123\n"
    "\n"
    "### Other\n"
    "\n"
    "- pipeline #23\n"
    "- workflow\n"
    "- refactoring code\n"
    "\n"))


(def changelog000-text (slurp "./test/assets/changelog000.md"))

(def changelog001-text (slurp "./test/assets/changelog001.md"))

(def changelog002-text (slurp "./test/assets/changelog002.md"))

(def changelog003-text (slurp "./test/assets/changelog003.md"))

(def changelog004-text (slurp "./test/assets/changelog004.md"))

(def changelog005-text (slurp "./test/assets/changelog005.md"))

(def changelog006-text (slurp "./test/assets/changelog006.md"))

(def changelog007-text (slurp "./test/assets/changelog007.md"))


(def changelog000-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED]]))


(def changelog001-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.17.0"]
       [:DATE "2023-04-14"]]
      [:ADDED
       [:LONG-DESC
        [:PARAGRAPH
         [:TEXT "new git hook mechanism "]
         [:ISSUE-REF [:ISSUE-ID "96"]]]
        [:PARAGRAPH
         [:TEXT "This commit adds a new mechanism how to use git hooks in projects using clci. A hook will send a trigger which is then processed using the clci workflow mechanism."]]
        [:PARAGRAPH
         [:TEXT "And one more line with a linebreak."]
         [:TEXT "So now what?"]]]]]]))


(def changelog002-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), "]
       [:TEXT "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.16.0"]
       [:DATE "2023-04-14"]]
      [:ADDED
       [:SHORT-DESC
        [:TEXT "new awesome feature "]
        [:ISSUE-REF [:ISSUE-ID "clci-77"]]]
       [:SHORT-DESC
        [:TEXT "another feature"]]
       [:SHORT-DESC
        [:TEXT "and again another awesome feature"]]]]]))


(def changelog003-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), "]
       [:TEXT "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.16.0"]
       [:DATE "2023-04-11"]]
      [:ADDED
       [:SHORT-DESC
        [:TEXT "new awesome feature "]
        [:ISSUE-REF [:ISSUE-ID "clci-77"]]]]]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.15.1"]
       [:DATE "2023-03-29"]]
      [:FIXED
       [:SHORT-DESC
        [:TEXT "fix invalid version parser "]
        [:ISSUE-REF [:ISSUE-ID "23"]]]]
      [:OTHER
       [:SHORT-DESC
        [:TEXT "update build pipeline for releases "]
        [:ISSUE-REF [:ISSUE-ID "11"]]]
       [:SHORT-DESC
        [:TEXT "cleanup dead code "]
        [:ISSUE-REF [:ISSUE-ID "12"]]]
       [:SHORT-DESC
        [:TEXT "refactor module structure "]
        [:ISSUE-REF [:ISSUE-ID "12"]]]]]]))



(def changelog004-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), "]
       [:TEXT "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.17.0"]
       [:DATE "2023-04-14"]]
      [:ADDED
       [:LONG-DESC
        [:PARAGRAPH
         [:TEXT "new git hook mechanism "]
         [:ISSUE-REF [:ISSUE-ID "96"]]]
        [:PARAGRAPH
         [:TEXT "This commit adds a new mechanism how to use git hooks in projects using clci. A hook will send a trigger which is then processed using the clci workflow mechanism."]]]]]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.16.0"]
       [:DATE "2023-04-11"]]
      [:ADDED
       [:SHORT-DESC
        [:TEXT "new awesome feature "]
        [:ISSUE-REF [:ISSUE-ID "clci-77"]]]]]
     [:RELEASE
      [:RELEASE-HEAD
       [:RELEASE-TAG "0.15.1"]
       [:DATE "2023-03-29"]]
      [:FIXED
       [:SHORT-DESC
        [:TEXT "fix invalid version parser "]
        [:ISSUE-REF [:ISSUE-ID "23"]]]]
      [:OTHER
       [:SHORT-DESC
        [:TEXT "update build pipeline for releases "]
        [:ISSUE-REF [:ISSUE-ID "11"]]]
       [:SHORT-DESC
        [:TEXT "cleanup dead code "]
        [:ISSUE-REF [:ISSUE-ID "12"]]]
       [:SHORT-DESC
        [:TEXT "refactor module structure "]
        [:ISSUE-REF [:ISSUE-ID "12"]]]]]]))


;; TODO
(def changelog005-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), "]
       [:TEXT "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]

     [:UNRELEASED]

     [:RELEASE
      [:RELEASE-HEAD [:RELEASE-TAG "0.17.0"] [:DATE "2023-04-14"]]
      [:ADDED
       [:LONG-DESC
        [:PARAGRAPH
         [:TEXT "new git hook mechanism "] [:ISSUE-REF [:ISSUE-ID "96"]]]
        [:PARAGRAPH
         [:TEXT "This commit adds a new mechanism how to use git hooks in projects using clci. A hook will send a trigger which is then processed using the clci workflow mechanism."]]]]]
     [:RELEASE
      [:RELEASE-HEAD [:RELEASE-TAG "0.16.0"] [:DATE "2023-04-11"]]
      [:ADDED
       [:SHORT-DESC
        [:TEXT "new awesome feature "]
        [:ISSUE-REF [:ISSUE-ID "clci-77"]]]]]
     [:RELEASE
      [:RELEASE-HEAD [:RELEASE-TAG "0.15.1"] [:DATE "2023-03-29"]]
      [:FIXED
       [:LONG-DESC
        [:PARAGRAPH
         [:TEXT "- fix invalid version parser "]
         [:ISSUE-REF [:ISSUE-ID "23"]]]
        [:PARAGRAPH
         [:TEXT "And another long description with multiple paragraphs."]]
        [:PARAGRAPH
         [:TEXT "This paragraph references issue "]
         [:ISSUE-REF [:ISSUE-ID "22"]]
         [:TEXT " and has a more than one line."]
         [:TEXT "Like it said, more than one line."]]]]
      [:OTHER
       [:SHORT-DESC
        [:TEXT "update build pipeline for releases "]
        [:ISSUE-REF [:ISSUE-ID "11"]]]
       [:SHORT-DESC
        [:TEXT "cleanup dead code "]
        [:ISSUE-REF [:ISSUE-ID "12"]]]
       [:SHORT-DESC
        [:TEXT "refactor module structure "]
        [:ISSUE-REF [:ISSUE-ID "12"]]]]]]))


(def changelog006-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), "]
       [:TEXT "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED
      [:FIXED
       [:SHORT-DESC [:TEXT "fix invalid version parser "] [:ISSUE-REF [:ISSUE-ID "23"]]]]
      [:OTHER
       [:SHORT-DESC [:TEXT "update build pipeline for releases "] [:ISSUE-REF [:ISSUE-ID "11"]]]
       [:SHORT-DESC [:TEXT "cleanup dead code "] [:ISSUE-REF [:ISSUE-ID "12"]]]
       [:SHORT-DESC [:TEXT "refactor module structure "] [:ISSUE-REF [:ISSUE-ID "12"]]]]]]))


;; TODO
(def changelog007-ast-expected
  '([:AST
     [:PREAMBLE
      [:PARAGRAPH
       [:TEXT "All notable changes to this project will be documented in this file."]]
      [:PARAGRAPH
       [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
     [:UNRELEASED
      [:FIXED
       [:SHORT-DESC
        [:TEXT "stupid mistake"]]]
      [:OTHER
       [:SHORT-DESC
        [:TEXT "more"]]
       [:SHORT-DESC
        [:TEXT "intermediate"]]
       [:SHORT-DESC
        [:TEXT "seems to fix missing issue ref"]]
       [:SHORT-DESC
        [:TEXT "wohoo almost done with "]
        [:ISSUE-REF [:ISSUE-ID "94"]]]
       [:SHORT-DESC
        [:TEXT "update docstrings"]]
       [:SHORT-DESC
        [:TEXT "more changelog parsing"]]
       [:SHORT-DESC
        [:TEXT "fns to add entries and prune"]]
       [:SHORT-DESC
        [:TEXT "generic tree dissoc"]]
       [:SHORT-DESC
        [:TEXT "add dissoc-node in tree"]]
       [:SHORT-DESC
        [:TEXT "valid tests"]]
       [:SHORT-DESC
        [:TEXT "stubbs for changelog parser"]]
       [:SHORT-DESC
        [:TEXT "change how tooling is used "]
        [:ISSUE-REF [:ISSUE-ID "101"]]]]]]))


(deftest changelog-text-validation
  (testing "Test if changelog test data can be parsed."
    (is (parseable? parser changelog-text-1))
    (is (parseable? parser changelog-text-2))
    (is (parseable? parser changelog-text-3))
    (is (parseable? parser changelog-text-4))
    (is (parseable? parser changelog-text-5))
    (is (parseable? parser changelog-text-6))
    (is (parseable? parser changelog000-text))
    (is (parseable? parser changelog001-text))
    (is (parseable? parser changelog002-text))
    (is (parseable? parser changelog003-text))
    (is (parseable? parser changelog004-text))
    (is (parseable? parser changelog005-text))
    (is (parseable? parser changelog006-text))
    (is (parseable? parser changelog007-text))))


(deftest changelog-parser-validation
  (testing "Test if AST from parser matches the expected for the example changelog files."
    (is (= changelog000-ast-expected (text->ast changelog000-text)))
    (is (= changelog001-ast-expected (text->ast changelog001-text)))
    (is (= changelog002-ast-expected (text->ast changelog002-text)))
    (is (= changelog003-ast-expected (text->ast changelog003-text)))
    (is (= changelog004-ast-expected (text->ast changelog004-text)))
    (is (= changelog005-ast-expected (text->ast changelog005-text)))
    (is (= changelog006-ast-expected (text->ast changelog006-text)))
    (is (= changelog007-ast-expected (text->ast changelog007-text)))))

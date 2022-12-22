(ns clci.conventional-commit-test
  "This module provides tests for the conventional commit parser."
  (:require
    [clci.conventional-commit :refer [grammar valid-commit-msg?]]
    [clojure.test :refer [deftest testing is]]
    [instaparse.core :as insta]))


(defn parseable?
  "Helper function to test if the given `msg` (string) can be parsed with the given `parser`."
  [parser msg]
  (not (insta/failure? (parser msg))))


(def valid-messages
  "Defines a collection of pairs of valid messages and their corresponding AST. Used to run tests."
  [;; 0
   ["feat: adding a new awesome feature"
    '([:TYPE "feat"] [:SUBJECT [:TEXT "adding a new awesome feature"]])]
   ;; 1
   ["chore: this is an intermediate commit with no meaning"
    '([:TYPE "chore"] [:SUBJECT [:TEXT "this is an intermediate commit with no meaning"]])]
   ;; 2
   ["fix: this commit fixes #RR-123"
    '([:TYPE "fix"] [:SUBJECT [:TEXT "this commit fixes "] [:ISSUE-REF [:ISSUE-ID "RR-123"]]])]
   ;; 3
   ["build(webapp): adding linter step to pipeline"
    '([:TYPE "build"] [:SCOPE "webapp"] [:SUBJECT [:TEXT "adding linter step to pipeline"]])]
   ;; 4
   ["feat(mobile): implements the new login page #RR-234"
    '([:TYPE "feat"] [:SCOPE "mobile"] [:SUBJECT [:TEXT "implements the new login page "] [:ISSUE-REF [:ISSUE-ID "RR-234"]]])]
   ;; 5
   ["feat: adding a new awesome feature\n\nThis commit implements the new feature of epic #RR-111. It is very good."
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "adding a new awesome feature"]]
      [:BODY
       [:TEXT "This commit implements the new feature of epic "]
       [:ISSUE-REF [:ISSUE-ID "RR-111"]]
       [:TEXT ". It is very good."]])]
   ;; 6
   ["fix: resolves input error of #RR-22\n\nThis commit implements a fix of input validation. It is very good :)"
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "resolves input error of "]
       [:ISSUE-REF [:ISSUE-ID "RR-22"]]]
      [:BODY
       [:TEXT "This commit implements a fix of input validation. It is very good :)"]])]
   ;; 7
   [(str
      "feat: switch to new API #RR-33\n\n"
      "BREAKING: With this commit we are switching to the new API. Please update your access token!\n\n"
      "See #RR-44 for more details.")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:BREAKING]
       [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]
      [:FOOTER
       [:TEXT "See "]
       [:ISSUE-REF [:ISSUE-ID "RR-44"]]
       [:TEXT " for more details."]])]
   ;; 8
   [(str
      "feat: switch to new API #RR-33\n\n"
      "BREAKING: With this commit we are switching to the new API. Please update your access token!\n\n"
      "See #RR-44 for more details.\n\n"
      "# Please enter a commit message")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:BREAKING]
       [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]
      [:FOOTER
       [:TEXT "See "]
       [:ISSUE-REF [:ISSUE-ID "RR-44"]]
       [:TEXT " for more details."]]
      [:GIT-REPORT
       [:COMMENT " Please enter a commit message"]])]
   ;; 9
   [(str
      "feat: switch to new API #RR-33\n\n"
      "BREAKING: With this commit we are switching to the new API. Please update your access token!\n\n"
      "See #RR-44 for more details.\n\n"
      "# Please enter the commit message for your changes. Lines starting\n"
      "#  with '#' will be ignored, and an empty message aborts the commit.\n"
      "#\n"
      "# Date:      Thu Dec 8 17:01:23 2022 +0100\n"
      "# On branch feat/rr-2\n"
      "# Changes to be committed:\n"
      "#       renamed:    ci-bb/src/clj/ci_bb/pod.clj -> ci-bb/src/clj/ci_bb/main.clj")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:BREAKING]
       [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]
      [:FOOTER
       [:TEXT "See "]
       [:ISSUE-REF [:ISSUE-ID "RR-44"]]
       [:TEXT " for more details."]]
      [:GIT-REPORT
       [:COMMENT " Please enter the commit message for your changes. Lines starting"]
       [:COMMENT "  with '#' will be ignored, and an empty message aborts the commit."]
       [:COMMENT ""]
       [:COMMENT " Date:      Thu Dec 8 17:01:23 2022 +0100"]
       [:COMMENT " On branch feat/rr-2"]
       [:COMMENT " Changes to be committed:"]
       [:COMMENT "       renamed:    ci-bb/src/clj/ci_bb/pod.clj -> ci-bb/src/clj/ci_bb/main.clj"]])]
   ;; 10
   ["chore: this does stuff\n\n"
    '([:TYPE "chore"]
      [:SUBJECT
       [:TEXT "this does stuff"]])]
   ;; 11
   ["fix: this fixes the bug in #123\n\n"
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "this fixes the bug in "]
       [:ISSUE-REF [:ISSUE-ID "123"]]])]])


(def invalid-messages
  "Defines a collection of invalid messages to test the parser."
  [;; 0
   "foo: adding a new awesome feature"
   ;; 1
   "feat(): adding a new awesome feature"
   ;; 2
   "adding a new awesome feature"
   ;; 3
   "feat:Adding a new awesome feature"
   ;; 4
   "feat:adding a new awesome feature"
   ;; 5
   "feat(this!): adding a new awesome feature"
   ;;
   ;; "feat: Adding a new awesome feature"
   ])


(deftest validate-messages
  (testing "Testing to validate correct commit messages."
    (let [parser	(insta/parser grammar)]
      (is (parseable? parser (get-in valid-messages [0 0])))
      (is (parseable? parser (get-in valid-messages [1 0])))
      (is (parseable? parser (get-in valid-messages [2 0])))
      (is (parseable? parser (get-in valid-messages [3 0])))
      (is (parseable? parser (get-in valid-messages [4 0])))
      (is (parseable? parser (get-in valid-messages [5 0])))
      (is (parseable? parser (get-in valid-messages [6 0])))
      (is (parseable? parser (get-in valid-messages [7 0])))
      (is (parseable? parser (get-in valid-messages [8 0])))
      (is (parseable? parser (get-in valid-messages [9 0])))
      (is (parseable? parser (get-in valid-messages [10 0]))
          (is (parseable? parser (get-in valid-messages [11 0])))))))


(deftest validate-messages-api
  (testing "Testing Method to validate commit messages trough pod's."
    (is (valid-commit-msg? (get-in valid-messages [0 0])))))


(deftest parse-messages
  (testing "Testing to parse valid commit messages."
    (let [parser  (insta/parser grammar)]
      (is (= (parser (get-in valid-messages [0 0])) (get-in valid-messages [0 1])))
      (is (= (parser (get-in valid-messages [1 0])) (get-in valid-messages [1 1])))
      (is (= (parser (get-in valid-messages [2 0])) (get-in valid-messages [2 1])))
      (is (= (parser (get-in valid-messages [3 0])) (get-in valid-messages [3 1])))
      (is (= (parser (get-in valid-messages [4 0])) (get-in valid-messages [4 1])))
      (is (= (parser (get-in valid-messages [5 0])) (get-in valid-messages [5 1])))
      (is (= (parser (get-in valid-messages [6 0])) (get-in valid-messages [6 1])))
      (is (= (parser (get-in valid-messages [7 0])) (get-in valid-messages [7 1])))
      (is (= (parser (get-in valid-messages [8 0])) (get-in valid-messages [8 1])))
      (is (= (parser (get-in valid-messages [9 0])) (get-in valid-messages [9 1])))
      (is (= (parser (get-in valid-messages [10 0])) (get-in valid-messages [10 1])))
      (is (= (parser (get-in valid-messages [11 0])) (get-in valid-messages [11 1]))))))


(deftest parse-invalid-messages
  (testing "Testing to parse invalid commit messages. Expect to fail!"
    (let [parser  (insta/parser grammar)]
      (is (insta/failure? (parser (get invalid-messages 0))))
      (is (insta/failure? (parser (get invalid-messages 1))))
      (is (insta/failure? (parser (get invalid-messages 2))))
      (is (insta/failure? (parser (get invalid-messages 3))))
      (is (insta/failure? (parser (get invalid-messages 4))))
      (is (insta/failure? (parser (get invalid-messages 5)))))))

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
       [:PARAGRAPH
        [:TEXT "This commit implements the new feature of epic "]
        [:ISSUE-REF [:ISSUE-ID "RR-111"]]
        [:TEXT ". It is very good."]]])]
   ;; 6
   ["fix: resolves input error of #RR-22\n\nThis commit implements a fix of input validation. It is very good."
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "resolves input error of "]
       [:ISSUE-REF [:ISSUE-ID "RR-22"]]]
      [:BODY
       [:PARAGRAPH 
        [:TEXT "This commit implements a fix of input validation. It is very good."]]])]
   ;; 7
   [(str
      "fix: resolves input error of #RR-22\n\n"
      "This commit implements a fix of input validation. It is very good.\n\n"
      "And another paragraph.\n")
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "resolves input error of "]
       [:ISSUE-REF [:ISSUE-ID "RR-22"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "This commit implements a fix of input validation. It is very good."]]
       [:PARAGRAPH
        [:TEXT "And another paragraph."]]])]
   ;; 8
   [(str
      "fix: resolves input error of #RR-22\n\n"
      "This commit implements a fix of input validation. It is very good.\n\n"
      "And another paragraph with an issue #RR-123.\n")
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "resolves input error of "]
       [:ISSUE-REF [:ISSUE-ID "RR-22"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "This commit implements a fix of input validation. It is very good."]]
       [:PARAGRAPH
        [:TEXT "And another paragraph with an issue "]
        [:ISSUE-REF [:ISSUE-ID "RR-123"]]
        [:TEXT "."]]])]
   ;; 9
   [(str
      "fix: resolves input error of #RR-22\n\n"
      "This commit implements a fix of input validation. It is very good.\n\n"
      "One more paragraph.\n\n"
      "And another paragraph with an issue #RR-123.\n")
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "resolves input error of "]
       [:ISSUE-REF [:ISSUE-ID "RR-22"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "This commit implements a fix of input validation. It is very good."]]
       [:PARAGRAPH
        [:TEXT "One more paragraph."]]
       [:PARAGRAPH
        [:TEXT "And another paragraph with an issue "]
        [:ISSUE-REF [:ISSUE-ID "RR-123"]]
        [:TEXT "."]]])]
   ;; 10
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "BREAKING CHANGE: will not work with library xzy before 0.2.4\n")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "BREAKING CHANGE"] 
        [:FOOTER-VALUE [:TEXT "will not work with library xzy before 0.2.4"]]]])]
   ;; 11
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "BREAKING CHANGE: will not work with library xzy before 0.2.4\n"
      "note: Thanks for all the fish.\n")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "BREAKING CHANGE"] 
        [:FOOTER-VALUE [:TEXT "will not work with library xzy before 0.2.4"]]]
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "note"] 
        [:FOOTER-VALUE [:TEXT "Thanks for all the fish."]]]])]
   ;; 12
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "note: Thanks for all the fish.\n")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "note"] 
        [:FOOTER-VALUE [:TEXT "Thanks for all the fish."]]]])]
   ;; 13
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "note: Thanks for all the fish.\n"
      "more: Don't forget to look at #RR-45!\n")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "note"] 
        [:FOOTER-VALUE [:TEXT "Thanks for all the fish."]]]
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "more"] 
        [:FOOTER-VALUE 
         [:TEXT "Don't forget to look at "] 
         [:ISSUE-REF [:ISSUE-ID "RR-45"]]
         [:TEXT "!"]]]])]
   ;; 14
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "note: Thanks for all the fish.\n"
      "see-docs: See [the docs](https://www.example.com/foo?id=22&page=3)\n"
      "more: Don't forget to look at #RR-45!\n")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "note"] 
        [:FOOTER-VALUE [:TEXT "Thanks for all the fish."]]]
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "see-docs"] 
        [:FOOTER-VALUE [:TEXT "See [the docs](https://www.example.com/foo?id=22&page=3)"]]]
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "more"] 
        [:FOOTER-VALUE 
         [:TEXT "Don't forget to look at "] 
         [:ISSUE-REF [:ISSUE-ID "RR-45"]]
         [:TEXT "!"]]]])]
   ;;;;;
   ;; 15
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "# Please enter a commit message")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:GIT-REPORT
       [:COMMENT " Please enter a commit message"]])]
   ;; 16
   [(str
      "feat: switch to new API #RR-33\n\n"
      "With this commit we are switching to the new API. Please update your access token!\n\n"
      "BREAKING CHANGE: will not work with library xzy before 0.2.4\n\n"
      "# Please enter a commit message")
    '([:TYPE "feat"]
      [:SUBJECT
       [:TEXT "switch to new API "]
       [:ISSUE-REF [:ISSUE-ID "RR-33"]]]
      [:BODY
       [:PARAGRAPH
        [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "BREAKING CHANGE"] 
        [:FOOTER-VALUE [:TEXT "will not work with library xzy before 0.2.4"]]]]
      [:GIT-REPORT
       [:COMMENT " Please enter a commit message"]])]
   ;; 17
   [(str
      "feat: switch to new API #RR-33\n\n"
      "BREAKING CHANGE: With this commit we are switching to the new API. Please update your access token!\n\n"
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
      [:FOOTER
       [:FOOTER-ELEMENT
        [:FOOTER-TOKEN "BREAKING CHANGE"]
        [:FOOTER-VALUE [:TEXT "With this commit we are switching to the new API. Please update your access token!"]]]]
      [:GIT-REPORT
       [:COMMENT " Please enter the commit message for your changes. Lines starting"]
       [:COMMENT "  with '#' will be ignored, and an empty message aborts the commit."]
       [:COMMENT ""]
       [:COMMENT " Date:      Thu Dec 8 17:01:23 2022 +0100"]
       [:COMMENT " On branch feat/rr-2"]
       [:COMMENT " Changes to be committed:"]
       [:COMMENT "       renamed:    ci-bb/src/clj/ci_bb/pod.clj -> ci-bb/src/clj/ci_bb/main.clj"]])]
   ;; 18
   ["chore: this does stuff\n\n"
    '([:TYPE "chore"]
      [:SUBJECT
       [:TEXT "this does stuff"]])]
   ;; 19
   ["fix: this fixes the bug in #123\n\n"
    '([:TYPE "fix"]
      [:SUBJECT
       [:TEXT "this fixes the bug in "]
       [:ISSUE-REF [:ISSUE-ID "123"]]])]
   ])


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
   (str "build: add pod manifest and enable bb lib\n"
     "\n"
     "This commit adds a manifest file for the library to be used as a pod and\n"
     "adds the babashka code to the path to enable use from other\n"
     "applications.\n\n"
     "# Please enter the commit message for your changes. Lines starting\n"
     "# with '#' will be ignored, and an empty message aborts the commit.\n")
   ])


(deftest validate-messages
  (testing "Testing to validate correct commit messages."
    (let [parser	(insta/parser grammar)]
      (doseq [[message _] valid-messages]
        (is (parseable? parser message)))
      )))


(deftest validate-messages-api
  (testing "Testing Method to validate commit messages trough pod's."
    (is (valid-commit-msg? (get-in valid-messages [0 0])))))


(deftest parse-messages
  (testing "Testing to parse valid commit messages."
    (let [parser  (insta/parser grammar)]
      (doseq [[message expected] valid-messages]
        (is (= expected (parser message ))
          )

        ))))


(deftest parse-invalid-messages
  (testing "Testing to parse invalid commit messages. Expect to fail!"
    (let [parser  (insta/parser grammar)]
      (doseq [message invalid-messages]
        (is (insta/failure? (parser message))))
      )))

(ns clci.conventional-commit
  "This module provides the tools to parse and validate commit messages
  to follow the Conventional Commit Specification."
  (:require
    [instaparse.core :as insta]))


(def grammar
  "A PEG grammar to validate and parse conventional commit messages."
  (str
    "<S>            =       (HEADER <EMPTY-LINE> FOOTER GIT-REPORT? <NEWLINE>*)
                            / ( HEADER <EMPTY-LINE> BODY (<EMPTY-LINE> FOOTER)? GIT-REPORT? <NEWLINE>*)
                            
                            / (HEADER <EMPTY-LINE> BODY GIT-REPORT? <NEWLINE>*)
                            / (HEADER GIT-REPORT? <NEWLINE>*);"
    "<HEADER>       =       TYPE (<'('>SCOPE<')'>)? <':'> <SPACE> SUBJECT;"
    "TYPE           =       'feat' | 'fix' | 'refactor' | 'perf' | 'style' | 'test' | 'docs' | 'build' | 'ci' | 'chore';"
    "SCOPE          =       #'[a-zA-Z0-9]+';"
    "SUBJECT        =       TEXT ISSUE-REF? TEXT? !'.';"
    "BODY           =       (!PRE-FOOTER PARAGRAPH) / (!PRE-FOOTER PARAGRAPH (<EMPTY-LINE> PARAGRAPH)*);"
    "PARAGRAPH      =       (ISSUE-REF / TEXT / (NEWLINE !NEWLINE))+;"
    "PRE-FOOTER     =       NEWLINE+ FOOTER;"
    "FOOTER         =       FOOTER-ELEMENT (<NEWLINE> FOOTER-ELEMENT)*;"
    "FOOTER-ELEMENT =       FOOTER-TOKEN <':'> <WHITESPACE> FOOTER-VALUE;"
    "FOOTER-TOKEN   =       ('BREAKING CHANGE' (<'('>SCOPE<')'>)?) / #'[a-zA-Z\\-^\\#]+';"
    "FOOTER-VALUE   =       (ISSUE-REF / TEXT)+;"
    "GIT-REPORT     =       (<EMPTY-LINE> / <NEWLINE>) COMMENT*;"
    "COMMENT        =       <'#'> #'[^\\n]*' <NEWLINE?> ;"
    "ISSUE-REF      =       <'#'> ISSUE-ID;"
    "ISSUE-ID       =       #'([A-Z]+\\-)?[0-9]+';"
    "TEXT           =       #'[^\\n\\#]+';"
    "SPACE          =       ' ';"
    "WHITESPACE     =       #'\\s';"
    "NEWLINE        =       <'\n'>;"
    "EMPTY-LINE     =       <'\n\n'>;"))


(def parser
  "Setup a parser for Conventional Commit messages."
  (insta/parser grammar))


(defn valid-commit-msg?
  "Predicate that takes a commit `msg` as string and returns true if the message satisfies the
  conventional commit specs."
  [msg]
  (-> parser
      (insta/parse msg)
      (insta/failure?)
      (not)))


(defn parse-only-valid
  "Takes a collection of commit `messages` and tries to parse them.
  All messages that can not get parsed are discarded and a collection
  with all valid messages in their AST format is returned."
  [messages]
  (->> messages
       (map #(insta/parse parser %))
       (remove insta/failure?)))


(defn msg->ast
  "Get the AST from a commit `msg`."
  [msg]
  (-> parser
      (insta/parse msg)))


(defn subtree-by-token
  "Implements a depth-first search on the abstract syntax `tree` to find
  the first subtree identified by the given `token`."
  [tree token]
  (let [token'        (first (first tree))
        child         (rest (first tree))
        tail          (rest tree)]
    (cond
      (= token' token)        child
      (coll? (first child))   (if-let [rec-res (subtree-by-token child token)]
                                rec-res
                                (subtree-by-token tail token))
      (empty? tail)           nil
      :else                   (subtree-by-token tail token))))


(defn get-type
  "Get the type of a CC message.
  Takes the abstract syntax `tree` of the message and returns the type as string."
  [tree]
  (-> (subtree-by-token tree :TYPE) first))


(defn get-scope
  "Get the scope of a CC message.
  Takes the abstract syntax `tree` of the message and returns the scope as string or nil."
  [tree]
  (-> (subtree-by-token tree :SCOPE) first))


(defn is-breaking?
  "Test if the CC message has breaking changes.
   Takes the abstract syntax `tree` of the message and returns true if the commit
   has a breaking change."
  [tree]
  (let [footer (subtree-by-token tree :FOOTER)]
    (loop [element  (first footer)
           tail     (rest footer)]
      (cond
        (= "BREAKING CHANGE" (second (second element)))  true
        (empty? tail) false
        :else
        (recur (first tail) (rest tail))))))

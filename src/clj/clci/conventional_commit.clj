(ns clci.conventional-commit
  "This module defines a PEG based grammer to validate and parse conventional commit messages
  including several functions to parse and validate a message satisfies the specification."
  (:require
    [instaparse.core :as insta]
    [pod-racer.core :as pod]))


(def grammar
  "A PEG grammar to validate and parse conventional commit messages."
  (str
    "<S>            =       (HEADER <EMPTY-LINE> FOOTER GIT-REPORT? <NEWLINE>*)
                            / ( HEADER <EMPTY-LINE> BODY (<EMPTY-LINE> FOOTER)? GIT-REPORT? <NEWLINE>*)
                            
                            / (HEADER <EMPTY-LINE> BODY GIT-REPORT? <NEWLINE>*)
                            / (HEADER GIT-REPORT? <NEWLINE>*);"
    "<HEADER>       =       TYPE (<'('>SCOPE<')'>)? <':'> <SPACE> SUBJECT;"
    "TYPE           =       'feat' | 'fix' | 'refactor' | 'perf' | 'style' | 'test' | 'docs' | 'build' | 'ops' | 'chore';"
    "SCOPE          =       #'[a-zA-Z0-9]+';"
    "SUBJECT        =       TEXT ISSUE-REF? TEXT? !'.';"
    "BODY           =       (!PRE-FOOTER PARAGRAPH) / (!PRE-FOOTER PARAGRAPH (<EMPTY-LINE> PARAGRAPH)*);"
    "PARAGRAPH      =       (ISSUE-REF / TEXT)+;"
    "PRE-FOOTER     =       NEWLINE+ FOOTER;"
    "FOOTER         =       FOOTER-ELEMENT (<NEWLINE> FOOTER-ELEMENT)*;"
    "FOOTER-ELEMENT =       FOOTER-TOKEN <':'> <WHITESPACE> FOOTER-VALUE;"
    "FOOTER-TOKEN   =       ('BREAKING CHANGE' (<'('>SCOPE<')'>)?) / #'[a-zA-Z\\-^\\#]+';"
    "FOOTER-VALUE   =       (ISSUE-REF / TEXT)+;"
    "GIT-REPORT     =       <EMPTY-LINE> COMMENT*;"
    "COMMENT        =       <'#'> #'[^\\n]*' <NEWLINE?> ;"
    "ISSUE-REF      =       <'#'> ISSUE-ID;"
    "ISSUE-ID       =       #'([A-Z]+\\-)?[0-9]+';"
    "TEXT           =       #'[^\\n\\#]+';"
    "SPACE          =       ' ';"
    "WHITESPACE     =       #'\\s';"
    "NEWLINE        =       <'\n'>;"
    "EMPTY-LINE     =       <'\n\n'>;"))


(defn valid-commit-msg?
  "Predicate that takes a commit `msg` as string and returns true if the message satisfies the
  conventional commit specs."
  [msg]
  (-> (insta/parser grammar)
      (insta/parse msg)
      (insta/failure?)
      (not)))


(defn msg->ast
  "Get the AST from a commit `msg`."
  [msg])

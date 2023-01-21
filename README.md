# ClCI

This library aims to provide utilities to improve the workflow of building production grade Clojure applications.

## Summary

The ClCI library provides tooling for the following use cases:

- Conventional Commit Message Linting

## Abstract

When building production grade software we need to ensure a high level of quality of our product. This can get more difficult over time as the code base grows in size and complexity. The number of external dependencies may increase which introduces an extra level of things to keep updated and look out for security problems. 

One of the ways to prevent degrading quality over time are conventions how to develop the product and using automations to ensure those conventions. Because - lets be honest - when nobody enforces those conventions we developers will get lazy and won't always keep to our own rules.

A second import part of keeping our product quality high is to use testing. Since there are other tools and guides how to do software testing this will not be covered by ClCI.

Independently of which exact measures the product- and development team uses to keep quality at a high level, the use of automations is highly recommended and certainly will make life easier. ClCI is build to be used in automations or on a developers local machine. It can be used from any CI/CD system. This project uses Github Actions and the provided examples will also focus on Github Actions.

## Documentation

We aim to keep all decisions made that influence this project as ADRs (Any Decision Record). You can find all ADRs created during the design and development of this project in the [ADR directory](./docs/adr/)

## Technical Overview

All tools should be able to be run not only on the JVM but also with Babashka. This keeps execution time down and eliminates external dependencies. 

## How to use

The library can either be used as a normal Clojure module by including it in your project. If you want to use it from Babashka you need to include it as a Pod:

```clojure
;; bb.edn
;; ...
  :deps  {clockworksio/clci    {:git/url "https://github.com/clockworksio/clci"
                                :git/tag "0.3.0" 
                                :git/sha "0662a1b3dd61079a09def6c71d357629b72df383"}}
;; ...
```

You can then use the pod i.e. to execute the commit linter from a Babashka task:
```clojure
(ns git-hooks
 (:require
  [clojure.term.colors :as c]
  [clci.conventional-commit :refer [valid-commit-msg?]]
  ; ...
))


(defmulti hooks (fn [& args] (first args)))

(defmethod hooks "commit-msg" [& _]
  (let [commit-msg (slurp ".git/COMMIT_EDITMSG")
        msg-valid? (true? (valid-commit-msg? commit-msg))]
    (if msg-valid?
      (println (c/green "\u2713") "commit message follows the Conventional Commit specification")
      (do
        (println (c/red "\u2A2F") "commit message does NOT follow the Conventional Commit specification")
        (println (c/red "Abort commit!"))
        (System/exit -1)
        ))))
```


## Development Workflow

Run the tests to check if your changes broke any existing functionality:
```sh
bb test
```
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

Unfortunately not all Clojure modules used by ClCI are compatible with Babashka. This makes it necessary to build some of the tools as Babashka Pods which can be run quickly from any Babashka script. This project uses GraalVM and the _native-image_ tool to build a standalone binary that can be called by Babashka using the Pods protocol. The Pods protocol interface is implemented using the [Podracer](https://github.com/justone/bb-pod-racer) library.

## How to use

The library can either be used as a normal Clojure module by including it in your project. If you want to use it from Babashka you need to include it as a Pod:

```clojure
;; bb.edn
;; ...
  :pods  {clci/pod    {:path   "./path/to/executable/clci-0.0.0.main"
                       :cache  false}}
;; ...
```

You can then use the pod i.e. to execute the commit linter from a Babashka task:
```clojure
(ns git-hooks
 (:require
  [clojure.term.colors :as c]
  [clci.pod :refer [valid-commit-msg?]]
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

### Build a Pod

First run the tests to check if your changes broke any existing functionality:
```sh
bb test
```

After all tests succeeded you can build a binary compatible with the pod protocol that can be used from any babashka script:
```sh
bb build pod
```
The pod binary will be placed `target/ci-bb-0.0.0.main`

**Hint**: The repository uses semantic versioning based on the commit message. As such the version is calculated in the automated build process and must not be changed manually!



# CC-CLJ


## How to use

The library can either be used as a normal Clojure module by including it in your project:
```clojure
(ns example.core
  (:require [ci-bb.conventional-commit :refer [valid-commit-msg?]]))

(valid-commit-msg? "chore: this is a valid message with an issue #123") ; -> true
```

To use the library from Babashka you need to load the library as a pod:
```clojure
#!/usr/bin/env bb
(require '[babashka.pods :as pods])

(pods/load-pod "ci-bb") ; path to the binary packaging the library w the pod protocol interface

(require '[ci-bb.pod :refer [valid-commit-msg?]])

(valid-commit-msg? "chore: this is a valid message with an issue #123") ; -> true

```




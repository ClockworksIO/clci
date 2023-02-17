# ClCI

This library aims to provide utilities to improve the workflow of building production grade Clojure applications.

**Early Version**: This project is still quite new and has most likely several bugs. Using clci in production is at your own risk. Please feel free to file any bugs you find.

## Summary

Please have a look at the full rationale behind this library below if you would like to know more about how I started this project. For a quick start just continue reading right here.

The ClCI library provides tooling for the following use cases:

- Conventional Commit Message Linting
- Lint the code
- Remove dead code and unused vars

## How to use clci

The library can either be used as a Babashka library by including it in your project:

```clojure
;; bb.edn
;; ...
  :deps  {clockworksio/clci    {:git/url "https://github.com/clockworksio/clci"
                                :git/tag "0.4.0" 
                                :git/sha "e8c12ca327721caaafa05a8688dfad2cd080b243"}}
;; ...
```

You can then use the module i.e. to execute the commit linter from a Babashka task:
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

## Rationale

When building production grade software we need to ensure a high level of quality for our product. This can get more difficult over time as the code base grows in size and complexity. The number of external dependencies may increase which introduces an extra level of things to keep updated and look out for potential problems. 

### Code Complexity and Quality

One of the ways to prevent degrading quality over time are conventions how to develop the product, use tools to evaluate the codebase in regards of bad patterns, security issues, etc. and using automations to ensure those conventions. Because - lets be honest - when nobody enforces those conventions we developers will get lazy and won't always keep to our own rules. These automations can either be run on an external CI system which will ensure that your source of truth - assuming that you are using a trunk based versioning approach - always enforces those rules. From my experience it is most times a great benefit to not only run automations in the CI system but also on the local machine before changes are committed and pushed to the remote. Since most projects these days use git as a versioning system, it is quite easy to use git hooks to run some tasks when changes get committed to the repository.

### Testing

Another important part of keeping our product quality high is to use testing. Since there are other tools and guides how to do software testing this will not be covered by clci. It only provides a simple boilerplate task to execute a testrunner.

### Versioning and Releases

In addition to tools and automations that help the team to keep a high quality standard other common tasks are a necessity to build and ship a product with success. Clci aims to help with some of those tasks. In a modern development process it is common to use an agile pattern and produce shippable releases as often as possible. This comes with the need of managing releases and product versions in a sane way without the need of complicated and manual work. A good approach to achive this goal is [Semantic Versioning](https://semver.org/) in combination with [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/). Clci uses those two conventions and provides tools to derive the version of a release based on the commit log. A further addition in the future will be automatic release creation using the Github API.

### Many Repositories vs. Single Monorepo

Most (new) projects today follow a non-monolithic pattern and usually consist of many smaller projects and applications. A simple example would be an application with a web based frontend, some kind of backend and a mobile app. To organize such a project in regards of the code organization comes with different options. Two of the most common choices are: 

- Each part of the project may use its own repository
- All code lives in the same repository

There are good reasons for both of the choices and the best choice (if any) will depend on the project itself (I believe there was not just a single religious war about that topic). When it comes to versioning it will require to choose how each of the components and the project as whole shall be versioned. With single repositories the obvious choice is each repository will follow its own releases and version numbers. When a component is deployed it may depend on a certain version of another component and the whole project team must find a way how to document these dependencies. Otherwise it can lead to incompatible components and ultimately a broken product. This can be the case especially when some server side component must implement some features used by a user facing component. In those cases it may be possible but most times not desireable that the component depending on another component be be blocked before it can get deployed. This problem can unfortunately not be solved by the next approach when using a Monorepo.

A project using a Monorepo can use the simple and straight forward way to always release everything. Lets assume that this project has four components but only one component has changed. This will trigger a new release and all components will be build and later deployed using this new version. This pattern makes it redundant to keep track which component version is compatible with which other component version because all components are simply compatible as long as they use the same version number (as stated before, you still must have an eye open to not create a release with one component requiring some functionality not present in the release!).
While this pattern is easy to apply it does break with the idea of semantic versioning and may create redundant buils of components with the same version number while the code they were build from is identical in those versions.

As a compromise a Monorepo can still use versions local to the projects in the repository. This will of cause still require the project team to keep track which versions are compatible.

Clci will provide some tools for Monorepo management in regards of the features provided by clci.

### Summary

Independently of which exact measures the product- and development team uses to keep quality at a high level, the use of automations is highly recommended and certainly will make life easier. Clci is build to be used in automations and on a developers local machine. It can be used from any CI/CD system. This project uses Github Actions and the provided examples will also focus on Github Actions.

Clci does not aim to be a replacement of other Clojure project tools like [leiningen](https://leiningen.org/) or [neil](https://github.com/babashka/neil. It merely attempts to fill the gap where it has been a lot of work to setup you projects code base with the different tools available.


## Documentation

The documentation can be found in the `docs/` directory. 

We aim to keep all decisions made that influence this project as ADRs (Any Decision Record). You can find all ADRs created during the design and development of this project in the [ADR directory](./docs/adr/).

Clci uses mkdocs to create a documentation.

## Technical Overview

All tools are intended to be used with Babashka. This keeps execution time down and eliminates external dependencies. It should still be possible to use most of the modules with plain Clojure by making some alterations.


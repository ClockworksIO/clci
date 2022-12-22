---
scope: Project
version: 1.0.0
date: 2022-12-16
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
---
# ADR004 - Conventional Commit Linter

## Summary

In [ADR001 - Conventional Commits](./adr001-conventional_commits.md) the decision was made that this project follows the Conventional Commit specification for writing commit messages. This ADR will explain why and how the ClCI project implements its own linter for commit messages to follow the Conventional Commit specs.

## Context

It is good practice to use a sane and properly standardized format to write commit messages. This does not only helps to keep the git history more readable but also allows to use several automations like automatic Changelog creation and automated release control using Semantic Versioning.

Until now It was necessary to use tools from other ecosystems - i.e. the Javascript ecosystem - to lint git commit messages on my machine utilizing the git _commit-msg_ hook. This has the big disadvante to have a mixed toolchain of Clojure(-Script) and Javascript code, configuration and dependencies. This does not only require additional tools and packages but also keeping everything up to date requires additional efford.

## Decision

Building a commit message linter in Clojure to enforce Conventional Commit messages. The linter must be compatible with Babashka to minimize startup time of the linter in comparison of loading a full JVM process each time the linter is used.

## Explanation

The library uses the awesome [Instaparse](https://github.com/Engelberg/instaparse) module to validate and parse commit messages. As of today the module is not compatible with babashka. Therefore it is necessary to create a pod and use the commit linter module over the pods protocol.
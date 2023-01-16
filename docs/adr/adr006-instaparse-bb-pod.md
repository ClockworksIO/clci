---
scope: Project
version: 1.0.0
date: 2022-12-21
authors: Moritz Moxter (moritz@clockworks.io)
status: superseded by adr006
---
# ADR006 - Instaparse Babashka Pod

## Summary

To validate and parse commit messages to follow the Conventional Commit specification (described in [ADR004](./adr004-conventional_commit_linter.md)) this library uses [Instaparse](https://github.com/Engelberg/instaparse). Because Instaparse is not yet compatible with Babashka it was required to build a custom pod from clci with Clojure and use this pod from any Babashka using the clci library. As of now this is no longer necessary. Instead we now use [instaparse.bb](https://github.com/babashka/instaparse.bb) which wraps a pod that provides the raw capabilities of Instaparse.

## Context

See the following [discussion about pods](https://github.com/babashka/babashka/discussions/1462) for further information on problems posed by building a pod from clci.

## Decision

Use the official pod and Babashka library for Instaparse instead of building a pod from clci which must then be included in any Babashka project which uses clci.

## Explanation

Using the official pod and Babashka library for Instaparse reduces the build complexity and use of clci.
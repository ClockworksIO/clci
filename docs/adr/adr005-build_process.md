---
scope: project
version: 1.0.1
date: 2022-12-21
authors: Moritz Moxter (moritz@clockworks.io)
status: superseded by adr006
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR005 - Build Process

**Scope**: project

**Status**: superseded by [ADR006](./adr006-instaparse_bb_pod.md)

**Version**: 1.0.1

**Date**: 2022-12-21

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR documents which toolchain was choosen for the build process of the project.

## Context

This library is intended to be used as 

- A library 
- An executable tool 
- A Babashka compatible pod

To create a build and release for all targets a well defined build chain is required. The build process should be 

- Easy to setup and start
- Easy to extend
- Reasonably Fast

## Decision

The build process is implemented using the [clojure/tools.build](https://github.com/clojure/tools.build) library in favor of other build tools like leiningen. The build process is defined in the `build.clj` file with some configuration taken directly from the `deps.edn` file. A build can and should be started using the provided Babashka tasks.

## Explanation

Using this small but powerful libray in combination with Babasha powered tasks to run different types of builds satisfies all three requirements given to the build chain.
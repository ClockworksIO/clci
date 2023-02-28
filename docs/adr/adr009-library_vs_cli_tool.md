---
scope: project
version: 1.0.0
date: 2023-03-02
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR009 - Library vs. CLI Tool

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-03-02

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

To help a developer to add clci to a new project and use it, the library provides a clean interface to install and use it from anu Clojure project.

## Context

A library aiming to help with developing and deploying a Clojure project needs to be useable. Being useable comes with the requirement to add the library and its tools to a new or existing project with ease.

Please see the corresponding [Github issue](https://github.com/ClockworksIO/clci/issues/26).

## Decision

Clci provides a function `core.-main` that can be executed from Babashka like `bb -m clci.core install`. This function will setup clci to be used from the project. 

## Explanation

Using a `-main` function that can be executed from any project with Babashka available is the easiest way to implement the requirement.

## Alternatives

Clci could provide an independend executable that could be installed on a developers machine. The executable would provide only functionality for bootstrapping a new project that uses clci. The binary would be build using GraalVM and the same codebase of clci.

This alternative did not make it as the final decision because it would be to complicated to maintain an extra build for a sole purpose that can easily be implemented using an executable main module.

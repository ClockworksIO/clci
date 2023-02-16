---
scope: project
version: 1.0.0
date: 2023-02-16
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR00 - A New Draft

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-02-16

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Context

Unused variables and other dead code will clutter the codebase over time and can lead to unnecessary warnings and errors. Refactoring the code to remove the unused vars by hand is error prone as a developer may not always spot all dead code. Using a (semi-)automated tool to detect and optionally remove dead code leads to less problems and time spent on finding those issues.

## Decision

Clci uses [carve](https://github.com/borkdude/carve) as tool to identify and remove unused variables and code. 

Carve is made available as a bb task that can be executed either by hand or as part of an automated workflow (i.e. git hooks or a CI pipeline).

## Explanation

Carve is an established tool for the task at hand and is based on [clj-kondo](https://github.com/borkdude/clj-kondo) and [rewrite-cljc](https://github.com/lread/rewrite-cljc-playground). It is compatible with both Clojure and Babashka and can be easily integrated into existing or new projects.

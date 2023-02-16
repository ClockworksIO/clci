---
scope: project
version: 1.0.0
date: 2023-02-16
authors: Moritz Moxter (moritz@clockworks.io)
status: draft
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR008 - Conventional Commit Footer Values

**Scope**: Project

**Status**: Draft

**Version**: 1.0.0-draft.1

**Date**: 2023-02-16

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR explains which key-value pairs should be used when writing commit messages in the message's footer.

## Context

In [ADR001 - Conventional Commits](./adr001-conventional-commits.md) we decided to use the Conventional Commit Specification for all commit messages written in this repository. The specification allows arbitrary footer values to be present. Those values can (and should) be used in a key-value manner to set additional machine readable information. This ADR describes a set of tokens and their value type that should be used throughout this project.

**Info**: When one of the described tokens is used then it MUST be used the way it is specified by this ADR. This does not prevent a developer from using additional tokens that are not specified by this ADR!

## Decision

This project uses the following footer tokens:

**Pull Request Reference**
`PR: #<number>` 
Reference a Pull Request with the given id. 

Example `PR: #30`

**Story Reference**
`Story: #<number>` 
Reference an issue of type (User-)Story from the commit. Can be used i.e. when a commit resolves an issue of type _Task_. 

Example: `Story: #33`

**Generic Informational Reference**
`See: ( #<number> | <uri> )`
Reference either an issue or an external resource using an uri for informational purposes.

Example: `See: #32` or `See: https://some.example.com/resource/2`
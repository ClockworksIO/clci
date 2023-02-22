---
scope: project
version: 1.0.1
date: 2023-02-22
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR012 - Test Coverage

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-02-22

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR reasons about the implementation of a tool to calculate test coverage.

## Context

It is a good practice to use tests to verify your code is working as expected. When writing tests it is not always clear if the tests actually test all functional code. To check if tests exist for all parts of the code a tool can be used.

## Decision

A tool to get the test coverage is added to Clci. The implementation uses the [cloverage](https://github.com/liquidz/antq) library.
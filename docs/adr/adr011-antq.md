---
scope: project
version: 1.0.1
date: 2023-02-22
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR011 - Find Outdated Dependencies

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-02-22

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR reasons about the implementation of a tool to check the depencendies for updates.

## Context

Outdated dependencies pose a potential security problem as they can contain vulnerabilities that have been fixed in a newer version of that dependency. It is also often a good practice to update to the latest version of a dependency early to avoid large changes that might be required when upgrading several version increments at once instead of several small changes over time.

## Decision

A tool to check if dependencies are outdated is added as a tool to Clci. The implementation uses the [antq](https://github.com/liquidz/antq) library.
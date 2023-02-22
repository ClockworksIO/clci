---
scope: project
version: 1.0.1
date: 2023-02-22
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR010 - Lines of Code

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-02-22

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR reasons about the implementation of a tool to get the lines of code of a project.

## Context

In each project several stakeholders are interested in metrics about the project and the code as part of the project. One of the metrics that can easily be calculated is the lines of code. 

In general a large codebase is more complex and it gets more difficult to maintain a high level of quality.

Though easy to get, the lines of code is a metric that must be treated with care. The lines of code, especially when taken over time, is an indicator of how much the codebase grows and can be used as a very rough estimate how complex the codebase may have grown. Still the lines of code are not an exact measurement about the complexity of the code base: a function may be written using 50 lines of code while the same functionality may be expressen in less then 20. 



## Decision

Getting the lines of code is added as a tool to Clci. The implementation uses the [linesofcode-bb](https://github.com/matthewdowney/linesofcode-bb) library.
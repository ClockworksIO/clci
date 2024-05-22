---
scope: project
version: 1.0.0
date: 2024-05-22
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR017 - Issue Types

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2024-05-22

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR describes the addition of a commandline based assistant to perform several tasks using clci.

## Context

A Developer needs to perform several tasks on a regular base. Those tasks include setting up a new repository with clci, adding products or bricks and much more. These tasks are implemented as Babashka tasks and each task can be executed using the commandline. Each task requires its own set of options. Executing these tasks should be as easy as possible for the Developer.

## Decision

Clci implements a simple yet efficient interactive dialog system that can guide the Developer through a task and promts the Developer with inputs to provide the required options to execute a task. The dialog is implemented using [gum](https://github.com/charmbracelet/gum).

## Comments

To allow easy headless execution of tasks, a future issue should add the additional functionality to pre-set the options as commandline arguments instead of the interactive dialog.
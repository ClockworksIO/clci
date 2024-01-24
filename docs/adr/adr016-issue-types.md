---
scope: project
version: 1.0.0
date: 2024-01-24
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR016 - Issue Types

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2024-01-24

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR sets the different types of issues used to specify the work to be done in the context of building clci.

## Context

New ideas and issues related to clci can come in different scopes and sizes. This definition will clearly state how the different issues are categorized depending on their size and scope. A clear definition helps all stakeholders to quickly understand and filter issues depending on their view on clci.

## Decision

There are four different issue types that form a hierarchical order: _Initiative_, _Feature_, _Story_ and _Task_. In addition the issue types _Bug Report_ and _Feature Request_ exist.

#### Bug Report
A Bug Report documents a fault in the functionality of clci and helps to solve the problem.

#### Feature Request
A Feature Request is the starting point to submit an idea about improvements and changes of clci that are not yet planned by any other issue.

#### Initiative
An Initiative adds extra business value to the product. An Initiative is more abstract and focused on the value that the new improved product will give to its stakeholders. It does not make any assumptions how the new value is implemented and may only reference very high level constraints (i.e. related regulatory constraints). The Initiative can be the base for many Features (or in some cases directly for Stories).

#### Feature
The Feature is a significant solution that will yield value to a stakeholder and plans the concrete implementation of this functionality. Its size is too large to be implemented easyly in the scope of what might be a sprint. Yet in difference to a Feature, the Feature can specify high level constraints for the implementation of the new functionality. A Feature is usually split into several smaller Stories.

#### Story
A Story is an item of work with a concrete and detailed scope and explanation that can be implemented inside a well defined timed window (i.e. a sprint). It must define constraints and acceptance criteria and technical details (when required) about the new funtionality. The specific work items a Story will resolve or steps required to resolve it can be split up into several Tasks.

#### Task
The Task is a small item of work that can be resolved in a short period of time.
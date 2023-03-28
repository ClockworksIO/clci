---
scope: project
version: 1.0.0
date: 2023-04-03
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR00 - Workflows and Actions

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-04-03

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR documents describes the introduction of native workflows in clci.

## Context

There are lots of recurring tasks when building software. Many of those can be automated. Clci has the goal to help the development team with this. It has already implemented lots of tools for that purpose. Those tools were build to be easy to compose and be used from any project using clci. Using those tools would still require to either run the tools by hand, include them into an existing third party CI/CD pipeline or workflow orchestration system or write scripts that are triggered i.e. by git hooks to run tools.

## Decision

Clci implements its own workflow system to run workflows and execute jobs.

## Explanation

Existing CI/CD systems like Jenkins or workflow orchestration systems such as Camunda are powerful but sometimes over the top, especially when the development team would like to run some automations directly on a local system. With the new workflow implementation we take the basic ideas of such systems and implement a small subset of their functionality following the idea of functional composition, immutability and pure functions from Clojure.

The workflow system defines three basic abstractions: _Actions_, _Workflows_ and _Jobs_.

### Actions 

The workflow systems allows the development team to define Actions. Actions are functions combined with metadata described using Clojure maps. Each action defines an optional set of named inputs and outputs. An Action should be pure whenever possible and only use outputs to pass data forward. Of cause Actions would not be very useful if they would not allow to perform side effects, i.e. re-format the code in place or create a release using an external service. Actions with side effects must be marked as impure.

A spec for actions and some examples can be found in the namespace `clci.actions`.


### Workflows and Jobs

A Workflow is a sequence of Actions run in a linear order one after the other. A workflow has at least one trigger defined. A trigger is used to identify when a workflow should be run. A trigger could be for example `:manual` to start a workflow manually or `:pre-commit` to run the workflow from the git pre-commit hook. 

The steps of a Workflow are calles Job. Each job is a set of data describing the Action with its inputs and a unique identifier within the workflow. Inputs defined for an Action in a Job can either be constants or reference the output of another job previously in the workflow.

Workflows will in the future also provide a mechanism to run them either in the context of the whole project, on specific products or for each product in isolation.

A full spec for Workflows and Jobs can be found in the namespace `clci.workflow`, some examples can be found in the test namespace `clci.workflow-test`.


## Consequences

Instead of running tools by hand and clci providing a rich command line interface to run those tools, the development team must use Actions to run tools. Actions can either be run as part of a workflow or as an adhoc job which comes closest to running a single task using the command line.

## Alternatives

- Run everything as a (Babashka) task from the command line
- Use an external workflow system
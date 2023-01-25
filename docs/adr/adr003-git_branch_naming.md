---
scope: project
version: 1.0.1
date: 2022-12-16
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---

# ADR003 - Git Branch Naming Convention

**Scope**: project

**Status**: accepted

**Version**: 1.0.1

**Date**: 2022-12-16

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

In [ADR002 - Git Workflow](./adr002-git_workflow.md) it was decided to use a Branch-Rebase-Squash workflow. In this ADR we decide how branch names should be formed.

## Context

In a Branch-Rebase-Squash workflow each item of work will be done in its own branch before it is merged back into the master branch. Each branch created requires a unique name to be identified not only locally on a developers machine but also on the remote.

## Decision

A new branch must be named using the following schema:

```text
<type>/<issue-id>(-<optional-slug)
```

Examples:
```
feat/clci-1
fix/fyn-123-memory-leak
```

## Explanation

Keeping branch names in a consistent schema will help in the overall development and quality assurance flow. Any stakeholder can clearly see what a branch is about. It also helps automatically linking an (external) issue management system with the repository (i.e. to automate linking Pull Requests with the issue tracker).

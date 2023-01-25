---
scope: project
version: 1.0.1
date: 2022-12-12
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---

# ADR001 - Conventional Commits

**Scope**: project

**Status**: accepted

**Version**: 1.0.1

**Date**: 2022-12-12

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This project uses Git as SCM and follows the [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/) specs when writing commit messages.

## Context

There are several developers working on the project at the same time. This makes it necessary to have a clean and consistent workflow how code is committed and how commit messages are structured.

## Decision

The project uses the Conventional Commit specification and follows the [Angular Guidelines](https://github.com/angular/angular/blob/22b96b9/CONTRIBUTING.md#-commit-message-guidelines). They can also be used to automatically generate a changelog. 

All developers are forced to write commit messages following the Conventional Commit specifications. This enforcement is put in place with git hooks and a commit message linter.

## Explanation

Please have a look at the full specification of [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#specification). This is a quick overview of the specs:

The commit message should be structured as follows:
```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Use one of the following types:

- **build**: Changes that affect the build system or external dependencies (example scopes: gulp, broccoli, npm)
- **ci**: Changes to our CI configuration files and scripts (example scopes: Travis, Circle, BrowserStack, SauceLabs)
- **docs**: Documentation only changes
- **feat**: A new feature
- **fix**: A bug fix
- **perf**: A code change that improves performance
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- **test**: Adding missing tests or correcting existing tests
- **chore**: for any other changes

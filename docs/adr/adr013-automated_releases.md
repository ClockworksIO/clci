---
scope: project
version: 1.0.0
date: 2023-03-28
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR013 - Automated Releases

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0

**Date**: 2023-03-28

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR explains how automated release creation was introduced into clci and how it works.

## Context

When building software, there comes the point in time when a new release of some sort has to be created. A release can be different things, a executable binary, a library or anything else that can be boxed as some shippable item. Traditionally a new release was planned ahead, assembled and shipped by a human. As software has grown more complex and automation was introduced many products use CICD workflow that creates releases automatically on a regular base without much or any human intervention. 

## Decision

Clci provides the functionality required to automatically create new versions and releases. Versions and releases are tracked and managed on a project base to allow disticnt releases for Monorepos hosting multiple projects. Clci exposes a `release` task that can be used either manually or as part of an automated CICD pipeline (i.e. Github Actions, Jenkins, etc.)

## Explanation

### Configuration

A repository using clci requires a `repo.edn` file to be present in the repo's root. This repository level configuration defines the source control system and provider as well as the projects that are part of the repo. 

The repo configuration must specify which version control system and provider are used. The SCM provider is also the platform used for release publishing. Currently only git as SCM with Github as provider are implemented. The following configuration is required:

| key                | Description                                        |
| ------------------ | -------------------------------------------------- |
| `:url`             | Url pointing to the remote git repository. String. |
| `:provider`        | The git provider Configuration. Map. 							|
| `:provider/name`   | The SCM provider. Keyword. Only `:github` allowed. |
| `:provider/repo`   | The name of the repository. String.                |
| `:provider/owner`  | The name of the repository's owner. String.        |


A project is a unit of things that belong together. Many times this is equal to a product. When working with multiple projects in a single repo, each project must reside in its own subdirectory specified by the `:root` key of the project's configuration entry. One exception is a repository that only contains a single project. In this case the root can be empty to point to the repo's root. Other mandatory fields are the `:version`, `:key` and `:release-prefix`:

| key                | Description                                                        |
| ------------------ | ------------------------------------------------------------------ |
| `:root`            | The root of the project. Path relative to the repo's root. String. |
| `:version`         | The current version of the project. String. Follows SemVer.        |
| `:key`             | A unique key to identify the project. Keyword.                     |
| `:release-prefix`  | A prefix to put in front of a release. String.                     |


Full example with a single project at the repo's root `repo.edn` taken from clci:
```clojure
{:scm
 {:type :git,
  :url "git@github.com:ClockworksIO/clci.git",
  :provider {:name :github, :repo "clci", :owner "ClockworksIO"}},
 :projects
 [{:root "", :version "0.13.15", :key :clci, :release-prefix "clci"}]}

```

### Update Version and Create a Release

Updating the version of each project and creating a new release is split into two tasks.

First a new version is set for each project. The version is derived by getting the latest release and the commit it references. All commits since this last release commit and the current commit are analyzed. For each commit the version impact is taken using the Conventional Commit specification. Commits not following the specification are ommitted. When the repo defines multiple projects, the commit history analysis identifies which of the projects is affected by the commit by analyzing the files changed by the commit. When files inside of a project's root are changed, then the change is considered relevant for the project. After the analysis the new version for each project is calculated and applied following the SemVer specification. The repo configuration is updated accordingly.

After having set new versions for the projects, the actual release can be created. Release creation will again fetch the latest release for each project and compare the version of the release with the version currently set in the repo configuration. If the repo defines a newer version than the latest release, a new release is created.

Both tasks are invoked on their own. It is strongly recommended to integrate this process in the CICD pipeline. 

!!! tip
    
    Have a look at the workflows of this project as an example how integration in the CICD process can look like.

## Comments

The implementation of clci is focused on creating new releases for projects written in a language from the Clojure family (Cojure, ClojureScript and Babashka). The general functionality of detecting changes in projects and updating the version in the `repo.edn` accordingly is agnostic of the language and build chains used for each project. This enables projects written in other languages to use clci for versioning and release management.

At this time clci only supports Github as SCM provider to release new product versions.

Release creation does not create any build artifacts! It only creates a new release using the SCM provider platform: That is creating a new tag on the relevant commit and marking it as a release. The creation of build artifacts will be a task for the future.
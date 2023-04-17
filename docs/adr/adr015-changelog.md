---
scope: project
version: 1.0.0
date: 2023-06-14
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---
# ADR015 - Changelog

**Scope**: Project

**Status**: Accepted

**Version**: 1.0.0-

**Date**: 2023-06-14

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This ADR describes the new functionality to generate and keep a Chagelog from the git commit history.

## Context

As time progresses and changes are made it is difficult to keep an overview about the changes and the order in which they were made. An interested party could always have a look at the commit history to get reminded about the changes. But this is difficult for non-developers and may be impossible for an end user without access to the code repository. 

In some cases a written history of the changes may be mandatory: Depending on the product's domain there can be government issued regulations that require a high level of documentation (i.e. in medical or financial). An organization striving towards a certification like ISO9001 must meet additional requirements in regards to documentation as well.

## Decision

Clci provides the means to generate a Changelog, that is a simple text document whith changes listed in chronological order. Clci will in turn use this new functionality to keep a Changelog of all changes made to clci.

## Explanation

The Changelog loosely follows the specification of [keepachangelog.com](https://keepachangelog.com/en/1.1.0/) and thus is structured in a preamble and a list of release sections. At the top of the releases list an optional _Unreleased_ section may be present to list changes that are not yet part of an official release.

To build a Changelog the commit history is used. A slice of the commit log from the latest release to the current commit on the current branch. The SCM Provider's API is used to pull the latest release and the commit referenced by the release.

A Changelog is created for each product in the repository. The Changelog creation process identifies the files changed by a commit and the products the commit is affecting. Only the Changelog of the affected products is updated. The reasoning behind this is the following: Most times a change made in the repository and thus a commit will only affect one product. It is therefore reasonable to assume that each product should keep it's own Changelog only with changes relevant to that specific product.
 
## Assumptions

The repositories commit messages follow the Conventional Commit Specifications.

## Consequences

+ Commit messages must follow the Conventional Commit Specification.
+ A Changelog is created that documents the changes over time

## Alternatives

### A manual Changelog

A Changelog could be created manually for every release or change put on trunk (that is assuming a trunk based workflow). 

This would be cumbersome and lead to errors because it is easy to forget to update the Changelog by hand.

### Create a Changelog from PRs

Instead of using the commit history directly, the Changelog could be created using the text body of a PR that pushes the new changes to trunk (that is assuming a trunk based workflow).

Using the commit history in favor of only PRs should allow a more finely grained Changelog and is more independed from the SCM used by the repository. I.e. when using git hooks to enforce commit messages will follow the Conventional Commit Specification, then the Changelog synthesis can expect this precondition is met with a high confidence. If instead the PR message's body is used to create a Changelog entry, it will be almost impossible (depending on the SCM Provider used) to enforce that the PR text follows a sane format. A Changelog synthesizer can still verify if the text conforms to the expected format, but it can not stop a Developer from using a malformed text in the first place.
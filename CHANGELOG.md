# Changelog

All notable changes to this project will be documented in this file.

The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

This commit resolves #94 to automatically create and update the
changelog. It provides an Action and an adhoc job to update the
changelog.

The changelog is generated from the git commit log by taking all commits
since the latest release which is retrieved from the scm provider. It
can either create a new release entry or add the changes to the
unreleased section of the changelog.

The changelog itself loosely follows the specification from
keepachangelog.com.


### Other

This commit changes how the tooling prodived by clci works and how it is
used. Most of the tooling was changed to be invoked using the mechanism
and ad-hoc Actions.

The tools are no longer invoked by babashka tasks, instead there is a
central clci task with its own dispatch method to run tools.



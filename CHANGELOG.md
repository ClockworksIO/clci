# Changelog

All notable changes to this project will be documented in this file.

The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- prepare releases for a single product reworked

### Other

- add missing specs
- add string utility to detect blanks
- prepare feature

## [0.18.1] - 2023-06-15

### Fixed

- dont set release update flag as default true

## [0.18.0] - 2023-06-15

### Added

This commit resolves #94 to automatically create and update thechangelog. It provides an Action and an adhoc job to update thechangelog.

The changelog is generated from the git commit log by taking all commitssince the latest release which is retrieved from the scm provider. Itcan either create a new release entry or add the changes to theunreleased section of the changelog.

The changelog itself loosely follows the specification fromkeepachangelog.com.


### Other

This commit changes how the tooling prodived by clci works and how it isused. Most of the tooling was changed to be invoked using the mechanismand ad-hoc Actions.

The tools are no longer invoked by babashka tasks, instead there is acentral clci task with its own dispatch method to run tools.



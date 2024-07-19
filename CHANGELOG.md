# Changelog

All notable changes to this project will be documented in this file.

The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.25.0] - 2024-07-19

### Added

This feature implements #clci-146


## [0.24.0] - 2024-07-19

### Added

This commit adds an extra (optional) argument to the release task tospecify a branch name to use as trunk. This allows to use a branchnaming scheme where the trunk is named different than _master_.

This commit also allows to set the trunk branch in the _repo.edn_ file.


### Other

Updates all dependencies and such resolves #141


## [0.22.1] - 2024-07-16

### Added

- add missing version argument #111
- adds missing '-' in tag and release name

## [0.22.0] - 2024-07-16

### Added

This commit immplements a new release mechanism. This improvement reduces the code complexity and makes releases a lot easier. It also implements versioning for bricks including creating brick version tags.



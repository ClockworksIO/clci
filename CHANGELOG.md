# Changelog

All notable changes to this project will be documented in this file.

The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.25.2] - 2024-07-19

### Fixed

This commit resolves the bug that no changelog updates were possible
when trunk is not master. This fix allows to either give an explicit
command line argument to specify the trunk branch when running the
release task or to use the trunk as configured in the repo.edn file. as
a fallback master is used as trunk.

This commit also updates the ci pipeline and removes alternative cache
keys.


## [0.25.1] - 2024-07-19

### Fixed

- resolves null error of #150

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



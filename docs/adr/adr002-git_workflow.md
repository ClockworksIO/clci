---
scope: project
version: 1.0.1
date: 2022-12-12
authors: Moritz Moxter (moritz@clockworks.io)
status: accepted
namespace: io.clockworks
document-type: adr@2.0.0
---

# ADR002 - Git Workflow: Simple Gitflow

**Scope**: project

**Status**: accepted

**Version**: 1.0.1

**Date**: 2022-12-12

**Authors**: Moritz Moxter (moritz@clockworks.io)

## Summary

This project uses Git as SCM and follows a Branch-Rebase-Squash workflow to keep the main branches clean.

## Context

There are several developers working on the project at the same time. This makes it necessary to have a clean and consistent workflow how code is committed and merged to a set of main branches. Otherwise there would be a high risk of merge conflicts and inconsistencies how commit messages are structured.

## Decision

The project uses a single _master_ branch that contains the stable and tested code of the project. Any changes to the code and related resources must be made in their separate branches. A new branch must be named using the fixed schema described in [ADR003 - Git Branch Naming Convention](./adr003-git_branch_naming.md)

A working branch is always branched from the master branch and is merged back into master after successful testing. The merge process requires the developer to create a Pull Request with the changes onto master. After a successful test and review the changes are merged into master.

The process on how a Pull Request must be created, reviewed and tested will be topic of another ADR.

As a result of this workflow all changes in the master branch are considered to be stable and ready to ship.


## Explanation

In detail explanation of the Branch-Rebase-Squash workflow:

### Branch-Rebase-Squash Workflow

Following the Branch-Rebase-Squash workflow will ensure a clean commit history for the main branches and helps to avoid conflicts between changes of different developers. Please follow this guideline:

First a developer has to clone the repository from [Github](https://github.com/clockworks/clci) and name the remote _upstream_:
```sh
git clone git@github.com:organization/project-repo.git --origin upstream
```

To add new features, fix bugs etc. always follow the Branch-Rebase-Squash workflow:

**1.**
Fetch the upstream to get all changes that may have been made by other developers since your last fetch:
```sh
git fetch upstream
```

**2.**
Merge the master branch from upstream into your local master branch:
```sh
git checkout master
git pull upstream/master
```

**3.**
Create a new branch for the work you would like to do:
```sh
git checkout -b feat/clci-123
```

**4.**
Make your changes on the new branch. Commit as often as you would like. Keep in mind that this branch is about one specific item of work. DON'T make changes not related to the work item (i.e. fix an error in another module).

**5.**
Again fetch all changes from upstream. This will ensure that your local copy and _upstream/master_ are up to date and include any changes that may have been made by other developers in the meantime.
```sh
git fetch upstream
# optional but convenient to have your local master branch show the changes of upstream/master
git checkout master
git pull upstream master
```

**6.**
Now it is finally time to squash and rebase the changes you made and put them to the remote. On the branch with your changes you can start rebasing against _upstream/master_. The goal is to squash all the commits you made in your branch into a single commit with a meaningful message. During development you will likely create many commits with small changes and messages like _fixing some thingy again_ or _added the string xy I forgot earlier_. Those messages are not very useful for another developer or reviewer in the future and should be omitted.
```sh
git checkout feat/clci-123
git rebase --interactive upstream/master
```
Start rebasing and squash all your commits into a single one. While performing the rebase you might run into merge conflicts. When that happens use `git status` to identify where the conflicts are, resolve them and add the changes. Then you can run `git rebase --continue` which will automatically take your changes and continue the rebasing. In the end you will be prompted with your favorite text editor to enter a meaningful commit message. The draft you get will include the commit messages of all former commits you squashed. Take them as a startingpoint and write a single commit message that explains what changes you made. The message MUST follow the Conventional Commit specifications. See [ADR-001](./adr001-conventional_commits.md) for a detailed description.

**7.**
Now you can push your branch to upstream and create a merge request. When you have pushed your branch to the remote earlier you might get an error message telling you about a non-matching commit history because you squashed all your commits. In that case you have to force pushing your changes.
```sh
git push feat/clci-123
# or when you have to force the push
git push --force-with-lease upstream feat/clci-123
```

**8.**
Create a merge request on Github for your changes into the _master_ branch.

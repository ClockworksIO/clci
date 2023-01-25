# Userguide

When building production grade software we need to ensure a high level of quality of our product. This can get more difficult over time as the code base grows in size and complexity. The number of external dependencies may increase which introduces an extra level of things to keep updated and look out for security problems. 

One of the ways to prevent degrading quality over time are conventions how to develop the product and using automations to ensure those conventions. Because - lets be honest - when nobody enforces those conventions we developers will get lazy and won't always keep to our own rules.

A second important part of keeping our product quality high is extensive testing. Since there are other tools and guides how to do software testing this will not be covered by clci in depth.

Independently of which exact measures the product- and development team uses to keep quality at a high level, the use of automations is highly recommended and certainly will make life easier. Clci is build to be used in automations or on a developers local machine. It can be used from any CI/CD system. This project uses Github Actions and the examples will also focus on Github Actions.

## Homogeneous Code Formatting

An easy way to begin improving the quality of you project's code is to begin using a formatter to ensure similar code styling thoughout the project. One of those tools is [cljstyle](https://github.com/greglook/cljstyle). Since _cljstyle_ is not compatible with the Babashka interpreter it can only be used as a Clojure tool.

We would suggest to add an alias to your `deps.edn` file with _cljstyle_ as extra dependency to use it:


```clojure
;; ...
:aliases 	{:format	{:deps 	{mvxcvi/cljstyle	{:mvn/version "0.15.0"}}}
			;; ...
  			}
;; ...
```

Afterwards you can run the formatter on your Clojure (and Babashka) files using `clj -M:format -m cljstyle.main fix` to enforce a homogeneous style on the project's code.

You can customize how the formatter will handle your code by creating a `.cljstyle` file in your projects root directory. A full set of available configuration options can be found on the _cljstyle_ Github page. We would recommend a minimal configuration of your formatter that at least sets an ignore pattern for your docs, any tooling cache (i.e. kondo) and build artifacts:

```clojure
;; cljstyle configuration
{:files {:ignore #{"target" "docs" ".clj-kondo"}}}
```

The formatter will not check your code for errors, it will only enforce clean styling of the clojure code written by you and the other developers working on the project.

## Linter - Kondo

One other piece to keep your code clean and readable is using a linter. [Kondo](https://github.com/clj-kondo/clj-kondo) is a linter for Clojure. It can either be run as a binary, a Clojure tool and lately directly from Babashka using the [clj-kondo-bb](https://github.com/clj-kondo/clj-kondo-bb) package.

Kondo statically analyzes your code and will point out potential errors and warns about smaller issues in your code i.e. unused imports. You can add a Babashka task to your project to run kondo.

We would recommend to add a small utility function to invoke kondo from Babashka i.e. in your project's Babshka `utils` module:

```clojure
(ns utils
  "BB utilties for my project."
  (:require
      [clj-kondo.core :as clj-kondo]))

(defn kondo-lint
  "Run kondo to lint the code.
  Raises a non zero exit code if any errors are detected. Takes an optional
  argument `fail-on-warnings?`. If true the function also raises a non zero
  exit code when kondo detects any warnings."
  ([] (kondo-lint false))
  ([fail-on-warnings?]
   (let [{:keys [summary] :as results} (clj-kondo/run! {:lint ["src"]})]
     (clj-kondo/print! results)
     (when (or
             (if fail-on-warnings? (pos? (:warning summary)) false)
             (pos? (:error summary)))
       (throw (ex-info "Linter failed!" {:babashka/exit 1}))))))
;; ...

```

In your projects `bb.edn` add _clj-kondo-bb_ as a dependency and create a task:
```clojure
;; ...
:deps {;; ...
       io.github.clj-kondo/clj-kondo-bb         {:git/tag "v2023.01.20" 
                                                 :git/sha "adfc7df"}}
;; ...
:tasks {;; ...
        lint        {:doc           "Run kondo to lint the code. Fail on errors."
                     :requires      ([utils :as u])
                     :task          u/kondo-lint}}
```

Now you can run kondo by simply running `bb lint` in your terminal.

## Git Hooks

A good place to start implementing automations in a project that already uses git as a version control system are [git hooks](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks). They can be used to automatically execute arbitrary scripts or executables when certain actions take place (i.e. when a commit is made).

Clci provides a small set of helpful functions to work with git hooks in its `clci.git-hooks-utils` namespace. Those utility functions can be used to setup git hook based automations using custom Babashka scripts you can customize for you project. 

[Mykhaylo Bilyanskyy](https://github.com/Blasterai) wrote [this nice article](https://blaster.ai/blog/posts/manage-git-hooks-w-babashka.html) on implementing git hooks with Babashka. Following his article you can create a `git-hooks` module in your projects Babashka directory resembling the following:

```clojure
(ns git-hooks
  "This module defines several functions that are invoked by Git hooks."
  (:require
    [babashka.process :refer [sh]]
    [clci.git-hooks-utils :refer [spit-hook changed-files]]
    [utils :refer [kondo-lint]]))

;; Use a multimethod to run different hooks.
(defmulti hooks (fn [& args] (first args)))

;; Install the hooks in you local repository.
(defmethod hooks "install" [& _]
  (spit-hook "pre-commit")
  (spit-hook "commit-msg"))


;; Git 'pre-commit' hook.
(defmethod hooks "pre-commit" [& _]
  (println "Running pre-commit hook...")
  (let [files (changed-files)]
    ;; run a clojure formatter on all staged files
	  (-> (sh "clj -M:format -m cljstyle.main fix") :out println)
    ;; run the linter, fail only on errors
    (kondo-lint)
	  ;; after the formatter changed the files they must be re-added to the commit
    (doseq [file files]
      (sh (format "git add %s" file)))))

;; Git 'commit-msg' hook.
(defmethod hooks "commit-msg" [& _]
  (println "Not implemented yet."))


;; Default handler to catch invalid hooks
(defmethod hooks :default [& args]
  (println "Unknown hook: " (first args)))

```
The module defines a multimethod with a method implementation for each git hook you would like to use. In this example we create mainly three methods:
- _install_: Setup and enable the given hooks. In this example they are 'pre-commit' and 'commit-message'
- _pre-commit_: Executed when `git commit` is invoked before the changes get commited into history
- _commit-message_: Run when `git commit` is invoked and a commit message was entered

We also define a default handler in case an unknown hook gets executed for clean error handling.

The _install_ method uses the utility functions of clci to install and enable hooks using the multimethod defined in this module. After running `bb hooks install` from your terminal the hooks are in place and get executed when you commit changes.

It gets more interesting in the _pre-commit_ method. This method is invoked whenever changes are comitted by a developer running `git commit`. The hook can implement various tests that must pass to execute the hook. If this method returns a non zero code a commit is aborted.

In this example implementation we use the hook to automate both the code formatting and linting we described above:
First the hook will collect all files staged for the commit. This information is required to automatically fix the styling of the Clojure files using _cljstyle_. The formatter is run in the same manner as we would when running in manually from the terminal. After fixing the code styling _kondo_ is run to detect potential errors in your code. Because _cljstyle_ might have changed the Clojure files staged in the commit, it is necessary to re-add the changed files. This is why we need the list of files staged for the commit which we collected at the beginning of the hook.

The hook will stop the commit if either the linter or formatter failed and thus prevent malformed code from beeing committed and pushed to the remote.


## Conventional Commit Linter

The next step in establishing a workflow to produce high quality software is to agree on a common format of how to write commit messages. There are lots of guides on how a good commit message should be written. A common denominator on this topic are [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

The Conventional Commit specification describes a format how a commit message must be structured. We will not explain the full specification here since the [official website](https://www.conventionalcommits.org/en/v1.0.0/) already explains the format in detail. In [ADR001](./../adr/adr001-conventional_commits.md) we have already summarized the format. 
Using a well defined format throughout your project will not only make commit logs easier to read but also allows several automations like 

- generate a Changelog and Releases Notes
- derive Version number for releases
- clean rules on referencing commits and other information (i.e. an issue tracker)

The clci project implements a parser for the Conventional Commit specification. The parser allows to validate if a given message follows the specs and optionally create an abstract representation of the commit message. The later can be used i.e. to create and format a Changelog text or Releases Notes.

Again it is good to establish this common format but it is even better to add some automation to enforce all commits follow the format. Once more we will use git hooks for this job. We take the already created `git-hooks` module and change the method for the _commit-msg_ hook as follows:

```clojure
(ns git-hooks
  (:require
    ;; ...
    [clci.conventional-commit :refer [valid-commit-msg?]]))
;; ...
(defmethod hooks "commit-msg" [& _]
  (let [commit-msg (slurp ".git/COMMIT_EDITMSG")
        msg-valid? (true? (valid-commit-msg? commit-msg))]
    (if msg-valid?
      (println "commit message follows the Conventional Commit specification")
      (do
        (println "Commit message does NOT follow the Conventional Commit specification")
        (println "Abort commit!")
        (System/exit -1)))))
```

Now everytime you commit changes to your repository the Conventional Commit linter is run on the commit message to ensure you are using the correct format.

!!! note

    Please keep in mind that the Conventional Commit linter only validates the format of the
    commit message but not the content!

## A word on Documentation

t.b.d.
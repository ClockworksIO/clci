# Get Started with clci

This page will guide you through the process of setting up clci in a new repository. The steps of this guide apply to all projects independend of the choice of technologies used to build the products hold inside of the repository.

!!! note

    Please be warned that the guide assumes you are using a Unix based system like Linux or a Mac. All steps should in general
    also work on Windows machines using the [WSL](https://en.wikipedia.org/wiki/Windows_Subsystem_for_Linux).
    It is also assumed that Github is the SCM provider you are using for the project (other SCM providers may be supported in the future).

## Prerequisites

As first step you need to have git and [Babashka](https://babashka.org/) installed on your machine. Please follow the official guide to install [Babashka](https://github.com/babashka/babashka#installation). You also need [gum](https://github.com/charmbracelet/gum) to use the assistant to setup and configure clci.


## Setup the Repository

To create a new project with clci as your tool of choice simply you first have to create a new git repository and add two configuration files and a directory for your project specific utilities.

```sh
git init example
cd example
mkdir bb
```

Next you need to add a `bb.edn` file with the following content (please use the latest version of clci as dependency):
```clojure
{:paths   ["bb"]
 :deps    {clockworksio/clci    {:git/url "https://github.com/clockworksio/clci"
                                 :git/tag "clci-0.19.0" 
                                 :git/sha "75f7edc3149495df59c89218a7e465c285198762"}}
 :tasks   {clci                 {:doc "Run clci.",
                                 :task (exec 'clci.core/-main)}}
 }
```

Next run the initial setup assistant with `bb clci setup` and follow the steps.

## Add A Product

After setting up the repository, the next step would usually be to add a product.

To add a new product run the product assistant with `bb clci product add` and follow these steps:

1. Select _Application_ as type
2. Select _Clojure_ as template
3. Choose a name for the app, i.e. "server"
4. Select the root directory. You can just stick with the default.
5. Select a product key. You can just stick with the default.
6. Select _Yes_ to enable the release automation for the product.
7. Select booth _nREPL_ and _clj-format_ to automatically add aliases to the `deps.edn` file of the new product for automations.

After setting the inputs the assistant will create the necessary directories and files for a new Clojure product and adds a new entry for the product in the `repo.edn` file in the products section.
The new product will already be prepared for automatic release creation (based on Semantic Versioning and Conventional Commits) using the tools build in clci. The last step creates an alias to run a nREPL server for the product and a second alias to run `cljstyle` to format the code of the product. The later can be invoked using the build-in workflow system of clci.

## Add A Brick

To add a Clojure Brick simply use the assistant with `bb clci brick add` and follow these steps:

1. Select _Clojure_ as template
2. Choose a name for the app, i.e. "database"
3. Select a brick key. You can just stick with the default.
4. Select booth _nREPL_ and _clj-format_ to automatically add aliases to the `deps.edn` file of the new brick for automations.

After setting the inputs the assistant will create the necessary directories and files for a new Clojure brick and adds a new entry for the brick in the `repo.edn` file in the bricks section.
The new brick will already be prepared for automatic release creation (based on Semantic Versioning and Conventional Commits) using the tools build in clci. The last step creates an alias to run a nREPL server for the brick and a second alias to run `cljstyle` to format the code of the brick. The later can be invoked using the build-in workflow system of clci.

## Setup Git Hooks

t.b.d.
(ns clci.changelog
  "This namespace provides all required tools to work with Changelogs.
   This includes foremost to parse and mutate changelogs following the
   specs of keepachangelog.com loosely.
   
   As such this module can
   
   - validate a Changelog's text to follow the spec
   - create an abstract syntax tree (AST) from a changelog
   - add new nodes to the changelog's AST
     - this includes adding new entries to the Unreleased section
     - create new release sections
   - fill the changelog section with data derived from the commit log"
  (:require
    [babashka.fs :refer [exists?]]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.github :as gh]
    [clci.release :refer [commit-affects-brick? commit-affects-product?
                          find-brick-latest-version-tag
                          get-product-latest-release transform-commit-log]]
    [clci.repo :as rp :refer [get-brick-by-key get-product-by-key valid-brick?
                              valid-product?]]
    [clci.util.core :refer [any join-paths postwalk-reduce]]
    [clci.util.tree :refer [ast-dissoc-nodes ast-get-node ast-insert-after]]
    [clojure.string :as str]
    [instaparse.core :as insta]))


(def grammar
  "A PEG grammar for changelogs loosely following keepachangelog.com specs."
  (str
    "<S>              =   AST;"
    "AST              =   PREAMBLE <EMPTYLINE> UNRELEASED (<EMPTYLINE> RELEASE)* <NEWLINE*>;"
    "PREAMBLE         =   <'# Changelog'> <EMPTYLINE> PARAGRAPH (<EMPTYLINE> PARAGRAPH)*;"
    "UNRELEASED       =   <UNRELEASED-HEAD> ((<EMPTYLINE> RELEASE-BODY) / &(<EMPTYLINE> <RELEASE>) / <#'$'>);"
    "UNRELEASED-HEAD  =   <'##'> <BLANK> <'['> <('UNRELEASED' | 'Unreleased')> <']'>;"
    "RELEASE          =   RELEASE-HEAD <NEWLINE> RELEASE-BODY ;"
    "<RELEASE-BODY>   =   ((ADDED | FIXED | OTHER) <NEWLINE>*)+;"
    "RELEASE-HEAD     =   <'##'> <BLANK> RELEASE-TAG <BLANK> <'-'> <BLANK> DATE <NEWLINE> &NEWLINE;"
    "RELEASE-TAG      =   <'['> #'[0-9]+\\.[0-9]+\\.[0-9]+' <']'>;"
    "DATE             =   #'\\d\\d\\d\\d-[01][0-9]-[0-3][0-9]';"
    "L3-PREFIX        =   '###' BLANK;"
    "ADDED            =   <L3-PREFIX> <('Added' | 'ADDED')> <EMPTYLINE> (SHORT-DESC-WRAP+ / LONG-DESC);"
    "FIXED            =   <L3-PREFIX> <('Fixed' | 'FIXED')> <EMPTYLINE> (SHORT-DESC-WRAP+ / LONG-DESC);"
    "OTHER            =   <L3-PREFIX> <('Other' | 'OTHER')> <EMPTYLINE> (SHORT-DESC-WRAP+ / LONG-DESC);"
    "<SHORT-DESC-WRAP>  = SHORT-DESC !#'.' / (SHORT-DESC <NEWLINE>);"
    "SHORT-DESC       =   <UL-PREFIX> (ISSUE-REF / TEXT)+;"
    "UL-PREFIX        =   '-' BLANK;"
    "LONG-DESC        =   LONG-DESC-WRAP;"
    "<LONG-DESC-WRAP>   = PARAGRAPH / (PARAGRAPH <NEWLINE> LONG-DESC-WRAP);"
    "PARAGRAPH        =   (ISSUE-REF <NEWLINE>? / TEXT <NEWLINE>? )+;"
    "ISSUE-REF        =   <'#'> ISSUE-ID;"
    "ISSUE-ID         =   #'([A-Za-z]+\\-)?[0-9]+';"
    "TEXT             =   #'[^\\n\\#]+';"
    "BLANK            =   <' '>;"
    "WHITESPACE       =   <#'\\s'>;"
    "NEWLINE          =   <'\n'>;"
    "EMPTYLINE        =   <'\n\n'>;"))


(def parser
  "Setup a parser for Conventional Commit messages."
  (insta/parser grammar))


(defn text->ast
  "Parse the given `text` and produce an ast."
  [text]
  (-> parser
      (insta/parse text)))


(defn changelog-exists?
  "Test if a changelog file exists.
   Accepts an optional `path` to the changelog. Defaults to the file _CHANGELOG.md_
   in the current directory."
  ([] (changelog-exists? "CHANGELOG.md"))
  ([path]
   (exists? path)))


(defn create-changelog-stub
  []
  (spit "CHANGELOG.md"
        (str
          "# Changelog\n"
          "\n"
          "All notable changes to this project will be documented in this file.\n"
          "\n"
          "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n"
          "\n"
          "## [UNRELEASED]\n")))



(defn with-long-descriptions?
  "Test if any of the given `commits` uses a long description."
  [commits]
  (any (fn [commit] (not (str/blank? (:body commit)))) commits))


(defn with-short-descriptions?
  "Test if any of the given `commits` uses a short description."
  [commits]
  (any (fn [commit] (str/blank? (:body commit))) commits))


(defn produce-changelog-release-entries
  "Create changelog entry ASTs from a commit log.
   Takes the `section` type used for the changelog, must be one of `:ADDED, :FIXED, :OTHER`.
   Also takes a function `f` used to filter commits to be included in this changelog release
   section and a collection of `commits`."
  [section f commits]
  (let [only-short?     (with-short-descriptions? commits)
        tf-short-desc   (fn [commit] [:SHORT-DESC (rest (ast-get-node (-> commit :ast) :SUBJECT))])
        tf-long-desc    (fn [commit] [:LONG-DESC (rest (ast-get-node (-> commit :ast) :BODY))])
        tf              (if only-short? tf-short-desc tf-long-desc)]
    (concat [section] (map tf (filter f commits)))))


(defn create-added-node
  "Create a new 'ADDED' entry for a changelog release entry.
   Takes the `commit-log` with amended(!) commits.
   Returns the ADDED entry as AST."
  [commit-log]
  (produce-changelog-release-entries
    :ADDED
    (fn [e] (= "feat" (-> e :ast (cc/get-type))))
    commit-log))


(defn create-fixed-node
  "Create a new 'FIXED' entry for a changelog release entry.
   Takes the `commit-log` with amended(!) commits.
   Returns the FIXED entry as AST."
  [commit-log]
  (produce-changelog-release-entries
    :FIXED
    (fn [e] (= "fix" (-> e :ast (cc/get-type))))
    commit-log))


(defn create-other-node
  "Create a new 'OTHER' entry for a changelog release entry.
   Takes the `commit-log` with amended(!) commits.
   Returns the OTHER entry as AST."
  [commit-log]
  (produce-changelog-release-entries
    :OTHER
    (fn [e] (not (some #{(-> e :ast (cc/get-type))} '("feat" "fix"))))
    commit-log))


(defn create-release-node
  "Create a new RELEASE node.
   Takes the `commit-log` with amended commits, the `tag` as string 
   and the `date` of the release as string.
   Returns a RELEASE node."
  [commit-log tag date]
  (let [release-head  [:RELEASE-HEAD [:RELEASE-TAG tag] [:DATE date]]
        not-empty?    (fn [node] (seq (second node)))
        added-node    (create-added-node commit-log)
        fixed-node    (create-fixed-node commit-log)
        other-node    (create-other-node commit-log)]
    (cond-> [:RELEASE release-head]
      (not-empty? added-node) (conj added-node)
      (not-empty? fixed-node) (conj fixed-node)
      (not-empty? other-node) (conj other-node))))


(defn create-unreleased-node
  "Create a new UNRELEASED node.
   Takes the `commit-log` with amended commits.
   Returns an UNRELEASED node."
  [commit-log]
  (let [not-empty?    (fn [node] (seq (second node)))
        added-node    (create-added-node commit-log)
        fixed-node    (create-fixed-node commit-log)
        other-node    (create-other-node commit-log)]
    (cond-> [:UNRELEASED]
      (not-empty? added-node) (conj added-node)
      (not-empty? fixed-node) (conj fixed-node)
      (not-empty? other-node) (conj other-node))))


(defn changelog-ast-add-release
  "Add a new release to the ast.
   Takes the `changelog-ast` of the current changelog,
   the `commit-log` with amended commits, the `tag` of the
   version as a string (following Semantic Versioning) and
   the `date` of the release.
   Adds a new release to the changelog with release entries derived
   from the commit log. Also purges the Unreleased section of the changelog.
   Returns an ast with the new release."
  [changelog-ast commit-log tag date]
  (-> changelog-ast
      (ast-dissoc-nodes :UNRELEASED)
      (ast-insert-after :PREAMBLE (create-unreleased-node []))
      (ast-insert-after :UNRELEASED (create-release-node commit-log tag date))))


(defn changelog-ast-add-unreleased
  "Add an Unreleased section to the ast.
   Takes the `changelog-ast` of the current changelog and
   the `commit-log` with amended commits.
   First purges the existing Unreleased section and fills the section
   with entries based on the commit log. If the commit log is empty, an
   empty Unreleased section is created.
   Returns an ast with the new Unreleased section."
  [changelog-ast commit-log]
  (-> changelog-ast
      (ast-dissoc-nodes :UNRELEASED)
      (ast-insert-after :PREAMBLE (create-unreleased-node commit-log))))


;; Implementation to convert the AST of a changelog into a text (file).
;; Utilizing the structure of the AST, the multimethod implements a handler
;; function for each node type, i.e. `:TEXT, :RELEASE, ...`. The functions
;; get recursively applied using the `ast->text` function that does a
;; postwalk on the AST while reducing the text fragments of all visited nodes.
(defmulti ast->text-impl (fn [node] (if (coll? node) (first node) :LEAF)))


(defmethod ast->text-impl :AST [node]
  (rest node))


(defmethod ast->text-impl :LEAF [node]
  node)


(defmethod ast->text-impl :PREAMBLE [node]
  (list "# Changelog\n\n" (rest node)))


(defmethod ast->text-impl :UNRELEASED [node]
  (list "## [Unreleased]\n\n" (rest node)))


(defmethod ast->text-impl :RELEASE [node]
  (rest node))


(defmethod ast->text-impl :RELEASE-HEAD [node]
  (list (str "## ") (second node) " - " (nth node 2) "\n\n"))


(defmethod ast->text-impl :RELEASE-TAG [node]
  (list "[" (second node) "]"))


(defmethod ast->text-impl :DATE [node]
  (second node))


(defmethod ast->text-impl :ADDED [node]
  (list "### Added\n\n" (rest node) "\n"))


(defmethod ast->text-impl :FIXED [node]
  (list "### Fixed\n\n" (rest node) "\n"))


(defmethod ast->text-impl :OTHER [node]
  (list "### Other\n\n" (rest node) "\n"))


(defmethod ast->text-impl :SHORT-DESC [node]
  (list "- " (rest node) "\n"))


(defmethod ast->text-impl :LONG-DESC [node]
  (list (rest node)))


(defmethod ast->text-impl :PARAGRAPH [node]
  (list (rest node) "\n\n"))


(defmethod ast->text-impl :NEWLINE [node]
  (list "\n" (rest node)))


(defmethod ast->text-impl :TEXT [node]
  (second node))


(defmethod ast->text-impl :ISSUE-REF [node]
  (list "#" (second node)))


(defmethod ast->text-impl :ISSUE-ID [node]
  (second node))


(defmethod ast->text-impl :default [node]
  node)


(defn ast->text
  "Convert an AST of a changelog into a changelog text.
   Takes the `ast` of the changelog and returns a text following the
   specification of a changelog loosely after keepachangelog.com.
   The resulting text is ready to be written into a changelog.md file."
  [ast]
  (first
    (postwalk-reduce ast->text-impl ast str)))


(defn update-changelog-ast!-impl
  "Update the changelog.
   Takes at least the `commit-log`.
   The optional `version-info` is a map with two keys `:version` and `:published`.
   If no release is provided, then all changes described in the commit-log will
   be added to the Unreleased section of the changelog. Otherwise a new Release
   is added to the changelog and all pending changes in the Unreleased section
   are erased.
   If no changelog exists an empty changelog will be created and filled with the
   new entries derived from the commit-log.
   Writes to the `changelog.md` file!"
  ([commit-log path version-info]
   (when-not (changelog-exists?)
     (create-changelog-stub))
   (let [changelog-path          (join-paths path "CHANGELOG.md")
         current-changelog-text  (slurp changelog-path)
         changelog-ast (text->ast current-changelog-text)
         changelog-ast  (if (some? version-info)
                          (changelog-ast-add-release changelog-ast commit-log (:version version-info) (:published version-info))
                          (changelog-ast-add-unreleased changelog-ast commit-log))]
     (spit changelog-path
           (ast->text changelog-ast))
     changelog-ast)))


(defn update-product-changelog!
  "Update the changelog of a product.
   Takes the `repo` following `:clci/repo` spec and the `product` following
   the `:clci.repo/product` spec. Optionally takes a `version-info` map with the keys
   `:version` and `:published` to create a new changelog entry for this specific version.
   When no version-info is supplied, the entries are added to the _Unreleased_ section of
   the changelog."
  ([repo product] (update-product-changelog! repo product nil))
  ([repo product version-info]
   (when-not (and (valid-product? product) (get-product-by-key (:key product) repo))
     (throw (ex-info "Product does not exist in repository." {:cause :product-does-not-exist})))
   (let [last-release                (get-product-latest-release repo product)
         commits-since-last-release  (->> (git/commits-on-branch-since {:since (get-in last-release [:commit :hash])})
                                          (transform-commit-log)
                                          (filter (fn [c] (commit-affects-product? c product))))]
     (update-changelog-ast!-impl commits-since-last-release (:root product) version-info))))


(defn update-brick-changelog!
  "Update the changelog of a brick.
   Takes the `repo` following `:clci/repo` spec and the `brick` following
   the `:clci.repo/brick` spec. Optionally takes a `version-info` map with the keys
   `:version` and `:published` to create a new changelog entry for this specific version.
   When no version-info is supplied, the entries are added to the _Unreleased_ section of
   the changelog."
  ([repo brick] (update-brick-changelog! repo brick nil))
  ([repo brick version-info]
   (when-not (and (valid-brick? brick) (get-brick-by-key (:key brick) repo))
     (throw (ex-info "Brick does not exist in repository." {:cause :brick-does-not-exist})))
   (let [gh-tags                       (gh/get-all-tag-refs (get-in repo [:scm :provider :owner]) (get-in repo [:scm :provider :repo]))
         tags                          (gh/tag-refs->tags gh-tags)
         last-brick-tag                (find-brick-latest-version-tag brick tags)
         commits-since-last-version    (->> (git/commits-on-branch-since {:since (:commit-sha last-brick-tag)})
                                            (transform-commit-log)
                                            (filter (fn [c] (commit-affects-brick? c brick))))]
     (update-changelog-ast!-impl commits-since-last-version (join-paths rp/brick-dir (:root brick)) version-info))))


(defn update-bricks-changelogs
  ""
  [repo])


(comment
  "Example how to use the `update-changelog` function."
  (def scm-owner  "clockworksIO")
  (def scm-repo   "clci")
  (def product    {:root "common" :key :common :release-prefix "common" :version "0.3.0"})
  (def brick      {:root "docker" :key :docker :version "0.12.1"})

  (def repo
    {:scm {:provider {:name :github
                      :owner scm-owner
                      :repo scm-repo}}
     :products [product]
     :bricks   [brick]})

  ;; update the changelog of a brick with no new version
  (update-brick-changelog! repo brick)
  
  ;; update the changelog of a product with a new release
  (update-product-changelog! repo product {:version "0.3.1" :published "2024-07-15"})
  )




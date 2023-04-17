(ns user
  ""
  (:require
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.release :as rel]
    [clci.repo :as rp]
    [clojure.core :as c]
    [clojure.string :as str]))


(require '[babashka.fs :as fs])

(require '[clojure.string :as str])


;; (require '[instaparse.core :as insta])

;; (require '[clojure.walk :refer [walk prewalk postwalk]])

(require '[clci.changelog :refer [text->ast changelog-exists? create-changelog-stub
                                  changelog-ast-add-release
                                  changelog-ast-add-unreleased
                                  ast->text]])


(require '[clci.util.core :refer [any mapv-r find-first join-paths]])


;; (require '[clci.util.tree :refer [ast-contains-node? ast-get-node ast-dissoc-nodes ast-insert-after]])


(require '[clci.release :as rel])
(require '[clci.git :as git])

(require '[clci.repo :as rp])
(require '[clci.conventional-commit :as cc])


(def example-ast
  "An example AST."
  [:AST
   [:PREAMBLE
    [:PARAGRAPH
     [:TEXT "All notable changes to this project will be documented in this file."]]
    [:PARAGRAPH
     [:TEXT "The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."]]]
   [:UNRELEASED
    [:OTHER
     [:SHORT-DESC
      [:TEXT "update build pipeline for releases "]
      [:ISSUE-REF [:ISSUE-ID "11"]]]]]
   [:RELEASE
    [:RELEASE-HEAD
     [:RELEASE-TAG "0.17.0"]
     [:DATE "2023-04-14"]]
    [:ADDED
     [:LONG-DESC
      [:PARAGRAPH
       [:TEXT "new git hook mechanism "]
       [:ISSUE-REF [:ISSUE-ID "96"]]]
      [:PARAGRAPH
       [:TEXT "This commit adds a new mechanism how to use git hooks in projects using clci. A hook will send a trigger which is then processed using the clci workflow mechanism."]]
      [:PARAGRAPH
       [:TEXT "And one more line with a linebreak."]
       [:TEXT "So now what?"]]]]]])



(def scm-owner "clockworksIO")
(def scm-repo        "clci")


;; (def repo
;;   {:scm {:provider {:name :github
;;                     :owner scm-owner
;;                     :repo scm-repo}}})

(def repo (rp/read-repo))


(def latest-release
  (rel/get-latest-release repo))


(def commits-since-release
  (git/commits-on-branch-since {:since (get-in latest-release [:commit :hash]) :branch "feat/clci-94"}))


(def amended-commits-since-release
  (rel/amend-commit-log commits-since-release))


amended-commits-since-release


(defn update-changelog!-impl
  "Update the changelog.
   Takes at least the `commit-log`.
   The optional `release` is a map conform to the `:clci.release/release` spec.
   If no release is provided, then all changes described in the commit-log will
   be added to the Unreleased section of the changelog. Otherwise a new Release
   is added to the changelog and all pending changes in the Unreleased section
   are erased.
   If no changelog exists an empty changelog will be created and filled with the
   new entries derived from the commit-log.
   Writes to the `changelog.md` file!"
  ;; ([commit-log product] (update-changelog!-impl commit-log product nil))
  ([commit-log product release]
   (when-not (changelog-exists?)
     (create-changelog-stub))
   (let [changelog-path          (join-paths (:root product) "CHANGELOG.md")
         current-changelog-text  (slurp changelog-path)
         changelog-ast (text->ast current-changelog-text)
         changelog-ast  (if (some? release)
                          (changelog-ast-add-release changelog-ast commit-log (:tag release) (:published release))
                          (changelog-ast-add-unreleased changelog-ast commit-log))]
     (spit changelog-path
           (ast->text changelog-ast))
     changelog-ast)))


(defn update-changelog!
  ""
  ([commit-log] (update-changelog! commit-log nil))
  ([commit-log release]
   (let [all-products  (rp/get-products)
         products-map  (->> commit-log
                            (map
                              (fn [commit]
                                (into (hash-map)
                                      (map
                                        (fn [product] [product (list commit)])
                                        (git/affected-products commit all-products)))))
                            (apply (partial merge-with into)))]
     (doseq [[product-key commits] products-map]
       (update-changelog!-impl commits (find-first (fn [p] (= (:key p) product-key)) all-products) release)))))


;; (->> amended-commits-since-release
;;   (map
;;     (fn [commit]
;;       (into (hash-map )
;;       (map
;;         (fn [product] [product (list commit)])
;;         (git/affected-products commit (rp/get-products))))
;;        ))
;;   (apply (partial merge-with into ))
;;   )




(update-changelog! amended-commits-since-release)

(ns clci.repo
  "This module provides methods to read and update the repo.edn file."
  (:require
    [babashka.fs :as fs]
    [clci.semver :as sv]
    [clci.util.core :as u :refer [slurp-edn pretty-spit! any]]
    [clojure.spec.alpha :as s]))



;;
;; Utilities to work with deps.edn ;;
;;


(defn read-deps
  "Read the deps.edn file."
  []
  (-> (slurp-edn "deps.edn")))


(defn write-deps!
  "Write the `data` to the deps.edn file.
   **warning**: Will override the file's contents!"
  [data]
  (pretty-spit! "deps.edn" data))



;;
;; Utilities to work with bb.edn ;;
;;

(defn read-bb
  "Read the bb.edn file."
  []
  (-> (slurp-edn "bb.edn")
      ;; TODO: Add checks that the mandatory fields are here!
      ))


(defn write-bb!
  "Write the `data` to the bb.edn file.
   **warning**: Will override the file's contents!"
  [data]
  (pretty-spit! "bb.edn" data))


(defn add-task
  "Add a new task to the bb.edn file.
  Takes the `name` which must be a string than can be cast to a symbol
  and identifies the babashka task and the function `t-fn` that is executed
  from the task. Also takes an optional `description` string for 
  the task and an optional list of requirements fo the task.
  
  The function that is executed by the task will be wrapped in a babashka
  (exec t-fn) statement and therefore must be fully qualified. It also should
  take an opts argument and implement the babashka cli API to parse optional
  arguments."
  [{:keys [name t-fn desc reqs]}]
  (-> (read-bb)
      (assoc-in
        [:tasks (symbol name)]
        (cond-> {:task (list 'exec t-fn)}
          (some? desc) (assoc :desc desc)
          (some? reqs) (assoc :requires reqs)))
      (write-bb!)))


;;
;; Utilities to work with repo.edn ;;
;;

(defn read-repo
  "Read the repo configuration."
  []
  (-> (slurp-edn "repo.edn")
      ;; TODO: Add checks that the mandatory fields are here!
      ))


(defn write-repo!
  "Write the `data` to the repo.edn file.
   **warning**: Will override the file's contents!"
  [data]
  (pretty-spit! "repo.edn" data))


(def valid-scm
  "A collection of valid scms."
  #{:git})


(def valid-scm-provider
  "A collection of valid scm provider."
  #{:github})


(defn- scm-url
  "Derive the scm url based on the scm `provider` and the `name` and `owner` of the repository."
  [provider name owner]
  (case provider
    :github (format "git@github.com:%s/%s.git" owner name)
    nil))


(defn- scm-provider-conf
  "Derive the scm provider configuration.
  Takes the scm `provider`, the `name` and `owner` of the repository."
  [provider name owner]
  (case provider
    :github {:name :github :repo name :owner owner}
    nil))


(defn- with-scm-provider
  "Add the scm provider to a base.
  Takes the following arguments:
  | key                 | description                         |
  | --------------------|-------------------------------------|
  | `base`              | (required!) A map representing the repo base.
  | `provider`          | (required!) The provider of the scm service, keyword. See the `valid-scm-provider` collection.
  | `repo-name`         | (required!) Name of the repository, string.
  | `repo-owner`        | (required!) Name of the repository owner, string."
  [base provider repo-name repo-owner]
  (assoc base :provider (scm-provider-conf provider repo-name repo-owner)))


(defn with-single-product
  "Add the basic product configuration for a single product at the
  repositories root to the base.
  Takes the repo `base`, map."
  [base & {:keys [initial-version] :or {initial-version "0.0.0-semver"}}]
  (when-not (sv/valid-version-tag? initial-version)
    (throw
      (ex-info
        "The initial version must follow the semantic versioning specification."
        {:reason :invalid-initial-version})))
  (assoc base
         :products
         [(cond-> {:root ""}
            (some? initial-version) (assoc :version initial-version))]))


(defn repo-base
  "Create a base for the repo."
  [opts]
  {:scm       (-> {:type    (:scm opts),
                   :url     (scm-url (:scm-provider opts) (:scm-repo-name opts) (:scm-repo-owner opts))}
                  (with-scm-provider (:scm-provider opts) (:scm-repo-name opts) (:scm-repo-owner opts)))})


(s/def :clci.repo.scm/type valid-scm)
(s/def :clci.repo.scm/url string?)
(s/def :clci.repo.scm.provider/name valid-scm-provider)
(s/def :clci.repo.scm.provider/repo string?)
(s/def :clci.repo.scm.provider/owner string?)


(s/def :clci.repo.scm/provider
  (s/keys :req-un [:clci.repo.scm.provider/name
                   :clci.repo.scm.provider/repo
                   :clci.repo.scm.provider/owner]))


(s/def :clci.repo/scm
  (s/keys :req-un [:clci.repo.scm/type
                   :clci.repo.scm/url]))


(def semver-tracking-options
  "All available options how semantic versioning is applied on the products in the repository."
  #{:distinct :combined})


(s/def :clci.repo.semver/tracking semver-tracking-options)


(s/def :clci.repo/semver
  (s/keys :opt-un [:clci.repo.semver/tracking]))


(s/def :clci.repo.product/root string?)
(s/def :clci.repo.product/key keyword?)
(s/def :clci.repo.product/version string?)
(s/def :clci.repo/product (s/keys :req-un []))

(s/def :clci.repo/products (s/coll-of :clci.repo/product))

(s/def :clci.repo/repo (s/keys :req-un [:clci.repo/scm :clci.repo/products]))


;; (s/def ::initial-version string?)
;; (s/def ::scm some?)
;; (s/def ::scm-provider some?)
;; (s/def ::scm-repo-name string?)
;; (s/def ::scm-repo-owner string?)


;; (s/fdef repo-base
;;   :args (s/cat :opts (s/keys :req-un [::scm :scm-provider ::scm-repo-name ::scm-repo-owner]))
;;   :ret map?
;; )

(defn update-product-version
  "Update the version in the repo.edn file.
  Takes the `version` as string and the product identifier.
  If only the version is supplied, the function assumes the repo has only a single product."

  [version product-key]
  (let [repo     (read-repo)
        idx      (u/find-first-index (:products repo) #(= (get % :key) product-key))
        product  (get-in repo [:products idx])]
    (->> (assoc-in repo [:products idx] (assoc product :version version))
         (pretty-spit! "repo.edn"))))


(defn affected-products
  "Tests which of the products are affected by the commit.
   Does so by checking if the changes happened in the root of each product.
   Takes an amended `commit` and the `products` of the repo.
   Returns a collection of product keys that are affected by the commit."
  [commit products]
  (if (= (count products) 1)
    (list (-> products first :key))
    (->> products
         (map (fn [prod]
                (when (any (fn [f] (fs/starts-with? f (:root prod))) (:files commit))
                  (:key prod))))
         (remove nil?))))


(defn product-affected-by-commit?
  "Test if the given `product` is affected by the given `commit`."
  [commit product]
  (some? (some #{(:key product)} (affected-products commit (list product)))))


(defn group-and-filter-commits-by-product
  "Create a map which products are affected by which commit.
   Takes the `commit-log` and `all-products` from the repo config. Maps over
   all commit log entries and tests which products are affected by each commit.
   Builds and returns a map where the keys are product keys and the value to
   each key is a list with all commits that are relevant to that product.
   
   !!! info
   
       You should provide a collection of commits limited back only to the
       latest release."
  [commit-log all-products]
  (->> commit-log
       (map
         (fn [commit]
           (into (hash-map)
                 (map
                   (fn [product] [product (list commit)])
                   (affected-products commit all-products)))))
       (apply (partial merge-with into))))


;;
;; MONOREPO FUNCTIONALITY ;;
;;

(defn get-paths
  "TODO: combine with utils from monorepo!"
  []
  ["src"])


(defn single-product?
  "Test if the repository contains more than a single product.
  This is the case when more than one entry exists in the repo.edn :products field."
  []
  (= (count (-> (read-repo) :products)) 1))


(defn get-products
  "Get all products."
  []
  (-> (read-repo) :products))


(defn- get-product-impl
  "Get a product by a specific attribute - implementation."
  [k v repo]
  (case k
    :key (u/find-first (fn [p] (= v (:key p))) (:products repo))
    :release-prefix (u/find-first (fn [p] (= v (:release-prefix p))) (:products repo))))


(defn- get-product
  "Get a product by a specific attribute."
  [k v repo]
  (get-product-impl k v repo))


(defn get-product-by-key
  "Get a product by its key."
  ([key] (get-product :key key (read-repo)))
  ([key repo] (get-product :key key repo)))


(defn get-product-by-release-prefix
  "Get a product by its release-prefix."
  ([prefix] (get-product :release-prefix prefix (read-repo)))
  ([prefix repo] (get-product :release-prefix prefix repo)))

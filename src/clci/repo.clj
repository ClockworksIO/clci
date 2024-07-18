(ns clci.repo
  "This module provides methods to read and update the repo.edn file."
  (:require
    [babashka.fs :as fs]
    [clci.util.core :as u :refer [slurp-edn pretty-spit! any]]
    [clojure.spec.alpha :as s]))


;;
;; Specs for the repo configuration
;;

(def valid-scm
  "A collection of valid scms."
  #{:git})


(def valid-scm-provider
  "A collection of valid scm provider."
  #{:github})


(def valid-product-types
  "All available and valid types for products."
  #{:application :library :other})


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


(s/def :clci.repo.product/root string?)
(s/def :clci.repo.product/key keyword?)
(s/def :clci.repo.product/version string?)
(s/def :clci.repo.product/type valid-product-types)
(s/def :clci.repo.product/no-release? boolean?)


(s/def :clci.repo/product
  (s/keys :req-un [:clci.repo.product/root
                   :clci.repo.product/key
                   :clci.repo.product/version
                   :clci.repo.product/type]
          :opt-un [:clci.repo.product/no-release?]))


(s/def :clci.repo.brick/name string?)
(s/def :clci.repo.brick/key keyword?)
(s/def :clci.repo.brick/version string?)


(s/def :clci.repo/brick
  (s/keys :req-un [:clci.repo.brick/name
                   :clci.repo.brick/key
                   :clci.repo.brick/version]))


(s/def :clci.repo/products (s/coll-of :clci.repo/product))

(s/def :clci.repo/bricks (s/coll-of :clci.repo/brick))

(s/def :clci.repo/repo (s/keys :req-un [:clci.repo/scm :clci.repo/products :clci.repo/bricks]))



;;
;; Definitions and Constants 
;;

(def brick-dir "bricks")


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


(defn valid-repo?
  "Predicate to test if a valid repo configuration exists."
  ([] (->> (read-repo) (s/valid? :clci.repo/repo)))
  ([repo] (s/valid? :clci.repo/repo repo)))


(defn get-paths
  "TODO: combine with utils from monorepo!"
  []
  ["src"])


(defn valid-brick?
  "Predicate to test if the given brick specification is valid."
  [brick]
  (s/valid? :clci.repo/brick brick))


(defn valid-product?
  "Predicate to test if the given product specification is valid."
  [product]
  (s/valid? :clci.repo/product product))


(defn single-product?
  "Test if the repository contains more than a single product.
  This is the case when more than one entry exists in the repo.edn :products field."
  []
  (= (count (-> (read-repo) :products)) 1))


(defn get-products
  "Get all products."
  ([] (-> (read-repo) :products))
  ([repo] (get repo :products [])))


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


(defn get-bricks
  "Get all bricks."
  ([] (-> (read-repo) :bricks))
  ([repo] (get repo :bricks [])))


(defn get-brick-by-key
  "Get a brick by its key."
  ([key] (get-brick-by-key key (read-repo)))
  ([key repo] (u/find-first (fn [b] (= key (:key b))) (:bricks repo))))


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


(defn with-new-product
  "Add a new `product` to the given `repo`."
  [repo product]
  (update repo :products #(conj % product)))


(defn with-new-brick
  "Add a new `brick` to the given `repo`."
  [repo brick]
  (update repo :bricks #(conj % brick)))


(defn add-product!
  "Add a new product to the repo configuration.
   Throws an exception if the given `product` violates the product specs."
  [product]
  (when-not (valid-product? product)
    (ex-info "Product specification invalid!" {:cause ::invalid-product-specification}))
  (write-repo! (with-new-product (read-repo) product)))


(defn add-brick!
  "Add a new brick to the repo configuration.
   Throws an exception if the given `brick` violates the brick specs."
  [brick]
  (when-not (valid-brick? brick)
    (ex-info "Brick specification invalid!" {:cause ::invalid-brick-specification}))
  (write-repo! (with-new-brick (read-repo) brick)))


(defn repo-base
  "Create a base for the repo."
  [opts]
  {:scm       (-> {:type    (:scm opts),
                   :url     (scm-url (:scm-provider opts) (:scm-repo-name opts) (:scm-repo-owner opts))}
                  (with-scm-provider (:scm-provider opts) (:scm-repo-name opts) (:scm-repo-owner opts)))
   :products []
   :bricks   []})



(defn update-product-version
  "Update the version in the repo.edn file.
  Takes the `version` as string and the product identifier."
  [version product-key]
  (let [repo     (read-repo)
        idx      (u/find-first-index (:products repo) #(= (get % :key) product-key))
        product  (get-in repo [:products idx])]
    (->> (assoc-in repo [:products idx] (assoc product :version version))
         (pretty-spit! "repo.edn"))))


(defn update-brick-version
  "Update the version in the repo.edn file.
  Takes the `version` as string and the brick identifier."

  [version brick-key]
  (let [repo     (read-repo)
        idx      (u/find-first-index (:bricks repo) #(= (get % :key) brick-key))
        brick  (get-in repo [:bricks idx])]
    (->> (assoc-in repo [:bricks idx] (assoc brick :version version))
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

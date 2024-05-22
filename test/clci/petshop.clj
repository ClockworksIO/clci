(ns clci.petshop
  "This module provides data to run tests with the petshop example data set."
  (:require
    [clci.test-utils :refer [commit-hash-re rfc3339-datetime-re commit-author-re gh-ref-node-id-re]]
    [clojure.spec.gen.alpha :as gen]
    [miner.strgen :as sg]))



(def org-name "PetworkInc")
(def repo-name "petshop")


(def products
  "The products of the petshop repo."
  [{:key :shop-backend :version "0.4.2" :root "shop-backend" :release-prefix "shop-backend"}
   {:key :storefront :version "0.7.1" :root "storefront" :release-prefix "storefront"}
   {:key :kiosk :version "0.0.0" :root "kiosk" :release-prefix "kiosk"}
   {:key :mobile :version "0.21.1-pre" :root "mobile" :release-prefix "mobile"}
   {:key :npdc :version "0.1.0-20200301.2" :root "npdc" :release-prefix "npdc"}])


(defn get-product
  [p-key]
  (->> products
       (filter (fn [{:keys [key]}] (= p-key key)))
       first))


(def bricks
  "The bricks of the petshop repo."
  [{:key :docker-client :version "0.4.0"}
   {:key :schema :version "0.12.1"}
   {:key :npdc-client :version "0.0.0"}
   {:key :specs :version "0.9.1"}])


(defn get-brick
  [b-key]
  (->> bricks
       (filter (fn [{:keys [key]}] (= b-key key)))
       first))


(def repo
  "Repo configuration for the petshop repo."
  {:products products
   :bricks bricks})


(def tag-names
  "The names of all existing tags as. Used to emulate the tags that would be returned
   by the Github API when fetching all existing tags.
   Tags are in reverse order, meaning the last entry of the vector is the latest tag name."
  ["shop-backend-0.3.1-rc1"
   "brick/docker-client-0.3.0"
   "storefront-0.7.0-20230101.1"
   "brick/schema-0.12.0"
   "brick/specs-0.9.0-rc2"
   "npdc-0.1.0-20200301.2"
   "shop-backend-0.4.1"
   "brick/docker-client-0.4.0"
   "brick/specs-0.9.1"
   "random-tag"
   "mobile-0.21.0"
   "brick/schema-0.12.1"
   "shop-backend-0.4.2"
   "mobile-0.21.1-pre"
   "storefront-0.7.1"])



(defn- mk-tag-ref
  [tag]
  (format "refs/tags/%s" tag))


(defn- mk-tag-node-id
  []
  (sg/string-generator gh-ref-node-id-re))


(defn- mk-tag-url
  [tag]
  (format "https://api.github.com/repos/%s/%s/git/refs/tags/%s"
          org-name
          repo-name
          tag))


(defn- mk-tag-obj-url
  [sha]
  (format "https://api.github.com/repos/%s/%s/git/commits/%s"
          org-name
          repo-name
          sha))


(defn gh-get-all-tag-refs-mock
  "Mocks the response of `clci.github/get-all-tag-refs` to get all available tags
   over the gh API."
  []
  (let [mk-ref      (fn [tag]
                      (let [sha (sg/string-generator commit-hash-re)]
                        {:ref (mk-tag-ref tag)
                         :node_id (mk-tag-node-id)
                         :url (mk-tag-url tag)
                         :object {:sha sha
                                  :type "commit"
                                  :url (mk-tag-obj-url sha)}}))]
    (mapv mk-ref tag-names)))


(defn get-latest-brick-tag
  [b-key]
  (case b-key
    :schema
    (let [tag  "brick/schema-0.12.1"
          sha  (sg/string-generator commit-hash-re)]
      {:ref       (mk-tag-ref tag)
       :node_id   (mk-tag-node-id)
       :url       (mk-tag-url tag)
       :object    {:sha sha
                   :type "commit"
                   :url (mk-tag-obj-url sha)}})
    :specs
    (let [tag  "brick/specs-0.9.1"
          sha  (sg/string-generator commit-hash-re)]
      {:ref       (mk-tag-ref tag)
       :node_id   (mk-tag-node-id)
       :url       (mk-tag-url tag)
       :object    {:sha sha
                   :type "commit"
                   :url (mk-tag-obj-url sha)}})
    :docker-client
    (let [tag  "brick/docker-client-0.4.0"
          sha  (sg/string-generator commit-hash-re)]
      {:ref       (mk-tag-ref tag)
       :node_id   (mk-tag-node-id)
       :url       (mk-tag-url tag)
       :object    {:sha sha
                   :type "commit"
                   :url (mk-tag-obj-url sha)}})))


(def latest-storefront-release
  {:version "0.7.1",
   :name "storefront-0.7.1",
   :tag {:name "storefront-0.7.1",
         :commit-sha "d66f9bad476d82b6ee254f8f80c5c627d5887615",
         :ref "refs/tags/storefront-0.7.1",
         :url "https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/storefront-0.7.1",
         :version "0.21.0"},
   :tag-name "storefront-0.7.1",
   :draft? false,
   :pre-release? false,
   :commit {:hash "d66f9bad476d82b6ee254f8f80c5c627d5887615"},
   :assets []})


(def latest-shop-backend-release
  {:version "0.4.2",
   :name "shop-backend-0.4.2",
   :tag {:name "shop-backend-0.4.2",
         :commit-sha "d66f9bad476d82b6ee254f8f80c5c627d5887615",
         :ref "refs/tags/shop-backend-0.4.2",
         :url "https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/shop-backend-0.4.2",
         :version "0.4.2"},
   :tag-name "shop-backend-0.4.2",
   :draft? false,
   :pre-release? false,
   :commit {:hash "d66f9bad476d82b6ee254f8f80c5c627d5887615"},
   :assets []})


(def latest-mobile-release
  {:version "0.21.1",
   :name "mobile-0.21.1",
   :tag {:name "mobile-0.21.1",
         :commit-sha "d66f9bad476d82b6ee254f8f80c5c627d5887615",
         :ref "refs/tags/mobile-0.21.1",
         :url "https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/mobile-0.21.1",
         :version "0.21.1"},
   :tag-name "mobile-0.21.1",
   :draft? false,
   :pre-release? false,
   :commit {:hash "d66f9bad476d82b6ee254f8f80c5c627d5887615"},
   :assets []})


(def latest-npdc-release
  {:version "0.1.0-20200301.2",
   :name "npdc-0.1.0-20200301.2",
   :tag {:name "npdc-0.1.0-20200301.2",
         :commit-sha "d66f9bad476d82b6ee254f8f80c5c627d5887615",
         :ref "refs/tags/npdc-0.1.0-20200301.2",
         :url "https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/npdc-0.1.0-20200301.2",
         :version "0.1.0-20200301.2"},
   :tag-name "npdc-0.1.0-20200301.2",
   :draft? false,
   :pre-release? false,
   :commit {:hash "d66f9bad476d82b6ee254f8f80c5c627d5887615"},
   :assets []})


(def get-git-commits-on-branch-a
  "Datasets for commits on a branch. Mocks the commits as they would be returned by
   `clci.git/commits-on-branch-since`.
   All commits are in order lastest to oldest: First item in the vector is the latest
   commit on the branch.
   - affected products: :storefront
   - affected bricks:
   - version increments: [:storefront -> [:patch]]"
  [{:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "fix: this commit repairs a minor flaw",
    :body ""
    :files
    ["storefront/src/storefront/core.clj"
     "storefront/src/storefront/router.clj"]}
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "chore: this commit does nothing meaningful at all",
    :body ""
    :files
    ["README.md"
     ".gitignore"
     "LICENSE"]}])


(def get-git-commits-on-branch-b
  "Datasets for commits on a branch. Mocks the commits as they would be returned by
   `clci.git/commits-on-branch-since`.
   All commits are in order lastest to oldest: First item in the vector is the latest
   commit on the branch.
   - affected products: :shop-backend
   - affected-bricks: :schema
   - version increments: [:shop-backend -> [:minor], :brick/schema -> [:minor]]"
  [{:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: this commit implements #PET-234",
    :body ""
    :files
    ["shop-backend/src/shop-backend/intents.clj"
     "shop-backend/src/shop-backend/db.clj"]}
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: this commit implements #PET-235",
    :body "This commit adds the new pet kind cat to the schema. Requirend for #PET-234."
    :files
    ["bricks/schema/src/core.clj"]}])

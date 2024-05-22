(ns clci.release-test
  "This module provides tests for release tool."
  (:require
    [clci.petshop :as petshop]
    [clci.release :as rel]
    [clci.test-data :as datasets]
    [clojure.test :refer [deftest testing is are]]))



(def fake-latest-releases-many
  "Fake latest releases for a repo with multiple products."
  [{:key    :pwa
    :commit (get-in datasets/raw-commits-dataset-1 [0 :hash])
    :tag     "0.0.0"
    :name    "0.0.0"}
   {:key    :backend
    :commit nil
    :tag     "0.0.0"
    :name    "0.0.0"}
   {:key    :common
    :commit (get-in datasets/raw-commits-dataset-1 [0 :hash])
    :tag     "0.0.0"
    :name    "0.3.0"}])


(comment
  "Expected versions for the example with many products using the
   _product-dataset-1_ and _raw-commits-dataset-1_:"
  {:pwa     "0.2.1"
   :backend "0.1.1"
   :common  "0.3.0"})


(def fake-latest-releases-single
  "Fake latest releases for a repo with a single product."
  [{:key    :app
    :commit (get-in datasets/raw-commits-dataset-2 [0 :hash])
    :tag     "0.0.0"
    :name    "0.0.0"}])


(comment
  "Expected versions using the single-product using the
   _product-dataset-2_ and _raw-commits-dataset-2_:"
  {:app     "2.2.1"})


(def gh-latest-releases-example-resp
  '({:draft       false
     :prerelease  false
     :name        "common-0.3.0"
     :tag_name    "common-0.3.0"
     :version     "0.3.0"
     :created_at  (gen/generate (sg/string-generator rfc3339-datetime-re))}
    {:draft       false
     :prerelease  false
     :name        "kuchen-0.1.0"
     :tag_name    "kuchen-0.1.0"
     :version     "0.1.0"
     :created_at  (gen/generate (sg/string-generator rfc3339-datetime-re))}
    {:draft       false
     :prerelease  false
     :name        "pwa-0.1.4"
     :tag_name    "pwa-0.1.4"
     :version     "0.1.4"
     :created_at  (gen/generate (sg/string-generator rfc3339-datetime-re))}
    {:draft       false
     :prerelease  false
     :name        "clci-0.13.15"
     :tag_name    "0.13.15"
     :version     "0.13.15"
     :created_at  (gen/generate (sg/string-generator rfc3339-datetime-re))}
    {:draft       false
     :prerelease  false
     :name        "0.3.2"
     :tag_name    "0.3.2"
     :version     "0.3.2"
     :created_at  (gen/generate (sg/string-generator rfc3339-datetime-re))}))


(def release-tags-example
  "Exampels to test the information extraction of version tags.
   Each entry is a vector of 
   `[full-tag-str expected-release-prefix-str expected-maj-min-patch-str expected-semver-str]`."
  [["release-1.2.4" "release" "1.2.4" "1.2.4"]
   ["some-release-34.2.44" "some-release" "34.2.44" "34.2.44"]
   ["1.2.4" nil "1.2.4" "1.2.4"]
   ["some-very-long-prefix-release-1.2.4" "some-very-long-prefix-release" "1.2.4" "1.2.4"]
   ["release-1.2.44-20240501.1" "release" "1.2.44" "1.2.44-20240501.1"]
   ["release-12.2.44-beta-1.202405010211" "release" "12.2.44" "12.2.44-beta-1.202405010211"]])


(deftest get-version-from-release-name
  (testing "Testing to get the prefix, version and mmp parts from a release name."
    (doseq [[full-tag prefix mmp version] release-tags-example]
      (are [x y] (= x y)
        prefix (rel/tag->release-prefix full-tag)
        mmp (rel/release-tag->maj-min-patch full-tag)
        version (rel/release-tag->semver-tag full-tag)))))


(deftest affected-products
  (testing "Testing if commit affects product using the petshop datasets."
    (is (rel/commit-affects-product? (first petshop/get-git-commits-on-branch-a) (petshop/get-product :storefront)))
    (is (not (rel/commit-affects-product? (second petshop/get-git-commits-on-branch-a) (petshop/get-product :storefront))))
    (is (not (rel/commit-affects-product? (first petshop/get-git-commits-on-branch-a) (petshop/get-product :mobile))))
    (is (not (rel/commit-affects-product? (second petshop/get-git-commits-on-branch-a) (petshop/get-product :mobile))))
    (is (not (rel/commit-affects-product? (first petshop/get-git-commits-on-branch-a) (petshop/get-product :shop-backend))))
    (is (not (rel/commit-affects-product? (second petshop/get-git-commits-on-branch-a) (petshop/get-product :shop-backend))))

    (is (not (rel/commit-affects-product? (first petshop/get-git-commits-on-branch-b) (petshop/get-product :storefront))))
    (is (not (rel/commit-affects-product? (second petshop/get-git-commits-on-branch-b) (petshop/get-product :storefront))))
    (is (not (rel/commit-affects-product? (first petshop/get-git-commits-on-branch-b) (petshop/get-product :mobile))))
    (is (not (rel/commit-affects-product? (second petshop/get-git-commits-on-branch-b) (petshop/get-product :mobile))))
    (is (rel/commit-affects-product? (first petshop/get-git-commits-on-branch-b) (petshop/get-product :shop-backend)))
    (is (not (rel/commit-affects-product? (second petshop/get-git-commits-on-branch-b) (petshop/get-product :shop-backend))))))


(deftest valid-version-tags
  (testing "Testing tag validation for product releases"
    (is (true? (rel/product-release-tag? "backend-1.2.3")))
    (is (true? (rel/product-release-tag? "back-end-1.2.3")))
    (is (true? (rel/product-release-tag? "backend-1.2.3-20230102.2")))
    (is (true? (rel/product-release-tag? "backend-1.2.3-rc1")))
    (is (false? (rel/product-release-tag? "backend-1.2.-rc1")))
    (is (false? (rel/product-release-tag? "brick/backend-1.2.3")))
    (is (false? (rel/product-release-tag? ""))))
  (testing "Testing tag validation for brick versions"
    (is (true? (rel/brick-version-tag? "brick/docker-1.2.3")))
    (is (true? (rel/brick-version-tag? "brick/docker-client-1.2.3")))
    (is (true? (rel/brick-version-tag? "brick/docker-client-1.2.3-rc33.1")))
    (is (false? (rel/brick-version-tag? "docker-1.2.3")))
    (is (false? (rel/brick-version-tag? "")))))

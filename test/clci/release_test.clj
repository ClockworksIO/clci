(ns clci.release-test
  "This module provides tests for release tool."
  (:require
    [clci.release :as rel]
    [clci.test-data :as datasets]
    [clci.util.core :refer [in? not-in?]]
    [clojure.test :refer [deftest testing is]]))



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


(deftest get-version-from-release-name
  (testing "Testing to get the version string from a release name"
    (is (= "1.2.4" (rel/release-name->version "release-1.2.4")))
    (is (= "34.2.44" (rel/release-name->version "some-release-34.2.44")))
    (is (= "1.2.4" (rel/release-name->version "1.2.4")))
    (is (= "1.2.4" (rel/release-name->version "some-very-long-prefix-release-1.2.4")))))


(deftest affected-products-many-products
  (testing "Testing helper function which products are affected by a commit - many products."
    (let [amended-commit-log (rel/amend-commit-log datasets/raw-commits-dataset-1)]
      (is (let [result (rel/affected-products (nth amended-commit-log 0) datasets/product-dataset-1)]
            (empty? result)))
      (is (let [result (rel/affected-products (nth amended-commit-log 1) datasets/product-dataset-1)]
            (in? result '(:pwa :minor))))
      (is (let [result (rel/affected-products (nth amended-commit-log 2) datasets/product-dataset-1)]
            (and
              (in? result '(:pwa :minor))
              (not-in? result '(:backend :minor)))))
      (is (let [result (rel/affected-products (nth amended-commit-log 3) datasets/product-dataset-1)]
            (and
              (in? result '(:backend :minor))
              (not-in? result '(:pwa :minor)))))
      (is (let [result (rel/affected-products (nth amended-commit-log 4) datasets/product-dataset-1)]
            (empty? result)))
      (is (let [result (rel/affected-products (nth amended-commit-log 5) datasets/product-dataset-1)]
            (and
              (in? result '(:pwa :patch))
              (in? result '(:backend :patch))))))))


(deftest derive-versions-many-products
  (testing "Testing to derive the current versions of app products based on the commit log - many products."
    (let [derived-versions (rel/derive-current-commit-all-versions-impl
                             (rel/amend-commit-log datasets/raw-commits-dataset-1)
                             datasets/product-dataset-1)]
      (is (= "0.2.1" (:pwa derived-versions)))
      (is (= "0.1.1" (:backend derived-versions)))
      (is (= "0.3.0" (:common derived-versions))))))



(deftest commit-version-increment-single-product
  (testing "Testing helper function how a commit increments a version - single product."
    (let [amended-commit-log (rel/amend-commit-log datasets/raw-commits-dataset-2)]
      (is (nil? (rel/derive-version-increment (-> amended-commit-log (nth 0) :ast))))
      (is (=
            (rel/derive-version-increment (-> amended-commit-log (nth 1) :ast))
            :major))
      (is (= (rel/derive-version-increment (-> amended-commit-log (nth 2) :ast)) :minor))
      (is (let [result (rel/derive-version-increment (-> amended-commit-log (nth 3) :ast))]
            (= result :minor)))
      (is (let [result (rel/derive-version-increment (-> amended-commit-log (nth 4) :ast))]
            (= result :patch))))))


(deftest derive-versions-single-product
  (testing "Testing to derive the current versions of app products based on the commit log - single product."
    (let [derived-version (rel/derive-current-commit-version-single-product-impl
                            (rel/amend-commit-log datasets/raw-commits-dataset-2)
                            (first datasets/product-dataset-2))]
      (is (= "2.2.1" (:app derived-version))))))


(deftest new-release-required?
  (testing "Testing if a new release is required based on the derived version and latest release."
    (let [derived-versions (rel/derive-current-commit-all-versions-impl
                             (rel/amend-commit-log datasets/raw-commits-dataset-1)
                             datasets/product-dataset-1)
          grouped-releases (-> gh-latest-releases-example-resp
                               (rel/group-gh-releases-by-prefix)
                               (rel/reduce-to-last-release))
          mk-fake-product  (fn [version] {:version version})]
      (is (rel/new-release-required?
            (mk-fake-product (get derived-versions :pwa))
            {:version (get-in grouped-releases ["pwa" :version])}))
      (is (rel/new-release-required?
            (mk-fake-product (get derived-versions :backend))
            {:version (get-in grouped-releases ["kuchen" :version])}))
      (is (not
            (rel/new-release-required?
              (mk-fake-product (get derived-versions :common))
              {:version (get-in grouped-releases ["common" :version])}))))))


(deftest prepare-new-releases
  (testing "Testing to derive which releases should be created for the repo based on changes."
    (let [products              (-> datasets/product-dataset-1)
          fake-releases         (-> gh-latest-releases-example-resp
                                    (rel/group-gh-releases-by-prefix)
                                    (rel/reduce-to-last-release))
          derived-versions      (rel/derive-current-commit-all-versions-impl
                                  (rel/amend-commit-log datasets/raw-commits-dataset-1)
                                  datasets/product-dataset-1)
          fake-repo             {:products (map (fn [p]
                                                  (assoc p :version (get derived-versions (:key p))))
                                                products)}
          result                (into (hash-map) (rel/prepare-new-releases-impl fake-repo fake-releases))]

      (is (contains? result "pwa"))
      (is (= "0.2.1" (get-in result ["pwa" :version])))
      (is (contains? result "kuchen"))
      (is (= "0.1.1" (get-in result ["kuchen" :version])))
      (is (not (contains? result "common"))))))

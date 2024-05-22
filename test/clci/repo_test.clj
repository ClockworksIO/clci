(ns clci.repo-test
  "This module provides tests for the `git` module."
  (:require
    [clci.petshop :as petshop]
    [clci.repo :as rp]
    [clci.test-data :as datasets]
    [clci.util.core :refer [same-elements?]]
    [clojure.test :refer [deftest testing is]]))


(deftest get-product-by-attribute
  (testing "Testing utility to get a product by its key"
    (is (map? (rp/get-product-by-key :storefront petshop/repo)))
    (is (map? (rp/get-product-by-key :shop-backend petshop/repo)))
    (is (nil? (rp/get-product-by-key :i-do-not-exist petshop/repo))))
  (testing "Testing utility to get a product by its release prefix"
    (is (map? (rp/get-product-by-release-prefix "storefront" petshop/repo)))
    (is (map? (rp/get-product-by-release-prefix "shop-backend" petshop/repo)))
    (is (nil? (rp/get-product-by-release-prefix "i-do-not-exist" petshop/repo)))))


(deftest get-brick-by-key
  (testing "Testing to get a brick by its key."
    (is (map? (rp/get-brick-by-key :docker-client petshop/repo)))
    (is (nil? (rp/get-brick-by-key :i-do-not-exist petshop/repo)))))


(deftest affected-products
  (testing "Testing which products are affected by a commit - repo with a single product"
    (is (same-elements?
          '(:app)
          (rp/affected-products
            (get datasets/raw-commits-dataset-2 0)
            datasets/product-dataset-2))))
  (testing "Testing which products are affected by a commit - repo with a multiple products"
    (is (same-elements?
          '()
          (rp/affected-products
            (get datasets/raw-commits-dataset-1 0)
            datasets/product-dataset-1)))
    (is (same-elements?
          '(:pwa)
          (rp/affected-products
            (get datasets/raw-commits-dataset-1 1)
            datasets/product-dataset-1)))
    (is (same-elements?
          '(:pwa)
          (rp/affected-products
            (get datasets/raw-commits-dataset-1 2)
            datasets/product-dataset-1)))
    (is (same-elements?
          '(:backend)
          (rp/affected-products
            (get datasets/raw-commits-dataset-1 3)
            datasets/product-dataset-1)))
    (is (same-elements?
          '()
          (rp/affected-products
            (get datasets/raw-commits-dataset-1 4)
            datasets/product-dataset-1)))
    (is (same-elements?
          '(:pwa :backend)
          (rp/affected-products
            (get datasets/raw-commits-dataset-1 5)
            datasets/product-dataset-1)))))

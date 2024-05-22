(ns clci.petshop-test
  (:require
    [clci.github :as gh]
    [clci.petshop :as petshop :refer [gh-get-all-tag-refs-mock latest-storefront-release get-product
                                      get-brick latest-shop-backend-release latest-mobile-release
                                      get-latest-brick-tag]]
    [clci.release :refer [find-product-latest-release-tag find-all-products-latest-release-tag
                          find-brick-latest-version-tag find-all-bricks-latest-version-tag
                          calculate-product-version calculate-products-version
                          calculate-bricks-version]]
    [clci.release :as rel]
    [clci.repo :refer [get-product-by-key get-brick-by-key]]
    [clojure.test :refer [deftest testing is are]]))


;; Testcase for release tags
;; Has tests for cases
;; - default semver tag (with older releas tags available)
;; - semver tag with pre release information
;; - a product that does not exist
;; - a product that has no release yet
;; 
;; Note: 
;;   For pre-release tags semver ordering cannot be guaranteed because there is
;;   no fixed convention how the prerelease part is structured (i.e. datetime, 
;;   incremental, ...). Therefore no test exists for that case.
(deftest find-products-latest-release
  (testing "Test finding the latest release for a specific product."
    (is (= "0.7.1"
           (:version (find-product-latest-release-tag
                       (get-product-by-key :storefront petshop/repo)
                       (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (= "0.4.2"
           (:version (find-product-latest-release-tag
                       (get-product-by-key :shop-backend petshop/repo)
                       (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (= "0.1.0-20200301.2"
           (:version
             (find-product-latest-release-tag
               (get-product-by-key :npdc petshop/repo)
               (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (= "0.21.1-pre"
           (:version (find-product-latest-release-tag
                       (get-product-by-key :mobile petshop/repo)
                       (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (nil?
          (find-product-latest-release-tag
            (get-product-by-key :kiosk petshop/repo)
            (gh/tag-refs->tags (gh-get-all-tag-refs-mock)))))
    (is (nil?
          (find-product-latest-release-tag
            (get-product-by-key :i-do-not-exist petshop/repo)
            (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))

  (testing "Test finding the latest release for all products."
    (let [expected {:storefront   "0.7.1"
                    :shop-backend "0.4.2"
                    :npdc         "0.1.0-20200301.2"
                    :kiosk        nil
                    :mobile       "0.21.1-pre"}
          actual   (find-all-products-latest-release-tag
                     (:products petshop/repo)
                     (gh/tag-refs->tags (gh-get-all-tag-refs-mock)))]
      (is (= (:storefront expected) (get-in actual [:storefront :version])))
      (is (= (:shop-backend expected) (get-in actual [:shop-backend :version])))
      (is (= (:npdc expected) (get-in actual [:npdc :version])))
      (is (= (:kiosk expected) (get-in actual [:kiosk :version])))
      (is (= (:mobile expected) (get-in actual [:mobile :version]))))))


;; Testcase for brick version tags
;; Has tests for cases
;; - default semver tag (with older releas tags available)
;; - semver tag with pre release information
;; - a brick that does not exist
;; - a brick that has no release yet
;; 
;; Note: 
;;   For pre-release tags semver ordering cannot be guaranteed because there is
;;   no fixed convention how the prerelease part is structured (i.e. datetime, 
;;   incremental, ...). Therefore no test exists for that case.
(deftest find-brick-latest-version
  (testing "Test finding the latestversion tag for a brick."
    (is (= "0.4.0"
           (:version (find-brick-latest-version-tag
                       (get-brick-by-key :docker-client petshop/repo)
                       (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (= "0.12.1"
           (:version (find-brick-latest-version-tag
                       (get-brick-by-key :schema petshop/repo)
                       (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (= "0.9.1"
           (:version (find-brick-latest-version-tag
                       (get-brick-by-key :specs petshop/repo)
                       (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))
    (is (nil?
          (find-brick-latest-version-tag
            (get-brick-by-key :npdc-client petshop/repo)
            (gh/tag-refs->tags (gh-get-all-tag-refs-mock))))))

  (testing "Test finding the latest release for all products."
    (let [expected {:docker-client  "0.4.0"
                    :schema         "0.12.1"
                    :specs          "0.9.1"
                    :npdc-client    nil}
          actual   (find-all-bricks-latest-version-tag
                     (:bricks petshop/repo)
                     (gh/tag-refs->tags (gh-get-all-tag-refs-mock)))]
      (is (= (:docker-client expected) (get-in actual [:docker-client :version])))
      (is (= (:schema expected) (get-in actual [:schema :version])))
      (is (= (:specs expected) (get-in actual [:specs :version])))
      (is (= (:npdc-client expected) (get-in actual [:npdc-client :version]))))))


(deftest calculate-brick-new-version
  (testing "Test to calculate the new version of a brick on Branch A"
    (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-a)]
      (is (= [0 12 1 nil] (rel/calculate-brick-version (get-latest-brick-tag :schema) (get-brick :schema))))
      (is (= [0 9 1 nil] (rel/calculate-brick-version (get-latest-brick-tag :specs) (get-brick :specs))))
      (is (= [0 4 0 nil] (rel/calculate-brick-version (get-latest-brick-tag :docker-client) (get-brick :docker-client))))))
  (testing "Test to calculate the new version of a brick on Branch B"
    (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-b)]
      (is (= [0 13 0] (rel/calculate-brick-version (get-latest-brick-tag :schema) (get-brick :schema))))
      (is (= [0 9 1 nil] (rel/calculate-brick-version (get-latest-brick-tag :specs) (get-brick :specs))))
      (is (= [0 4 0 nil] (rel/calculate-brick-version (get-latest-brick-tag :docker-client) (get-brick :docker-client)))))))


(deftest calculate-many-bricks-new-version
  (let [get-brick-version (fn [tags b-key]
                            (->> tags
                                 (filter
                                   (fn [[key _]] (= key b-key)))
                                 first
                                 second))]
    (testing "Test to calculate the new version of a brick on Branch A"
      (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-a)
                    clci.github/get-all-tag-refs (fn [& _] (gh-get-all-tag-refs-mock))]
        (let [versions (calculate-bricks-version petshop/repo)]
          (is (= [0 12 1 nil] (get-brick-version versions :schema)))
          (is (= [0 9 1 nil] (get-brick-version versions :specs)))
          (is (= [0 4 0 nil] (get-brick-version versions :docker-client))))))
    (testing "Test to calculate the new version of a brick on Branch B"
      (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-b)
                    clci.github/get-all-tag-refs (fn [& _] (gh-get-all-tag-refs-mock))]
        (let [versions (calculate-bricks-version petshop/repo)]
          (is (= [0 13 0] (get-brick-version versions :schema)))
          (is (= [0 9 1 nil] (get-brick-version versions :specs)))
          (is (= [0 4 0 nil] (get-brick-version versions :docker-client))))))))


(deftest calculate-product-new-version
  (testing "Test to derive the new version on branch A"
    (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-a)]
      (is (= [0 7 2] (calculate-product-version latest-storefront-release (get-product :storefront))))
      (is (= [0 4 2 nil] (calculate-product-version latest-shop-backend-release (get-product :shop-backend))))
      (is (= [0 21 1 "pre"] (calculate-product-version latest-mobile-release (get-product :mobile))))))
  (testing "Test to derive the new version on branch B"
    (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-b)]
      (is (= [0 7 1 nil] (calculate-product-version latest-storefront-release (get-product :storefront))))
      (is (= [0 5 0] (calculate-product-version latest-shop-backend-release (get-product :shop-backend))))
      (is (= [0 21 1 "pre"] (calculate-product-version latest-mobile-release (get-product :mobile)))))))


(deftest calculate-many-products-new-version
  (testing "Test to derive the new version on branch A"
    (with-redefs [clci.release/get-product-latest-release (fn [_ product]
                                                            (case (:key product)
                                                              :storefront petshop/latest-storefront-release
                                                              :shop-backend petshop/latest-shop-backend-release
                                                              :mobile petshop/latest-mobile-release
                                                              :npdc petshop/latest-npdc-release
                                                              nil))]
      (let [get-product-release (fn [releases p-key]
                                  (->> releases
                                       (filter
                                         (fn [[key _]] (= key p-key)))
                                       first
                                       second))]
        (with-redefs [clci.git/commits-on-branch-since        (fn [_] petshop/get-git-commits-on-branch-a)]
          (let [versions (calculate-products-version petshop/repo)]
            (is (= [0 7 2] (get-product-release versions :storefront)))
            (is (= [0 4 2 nil] (get-product-release versions :shop-backend)))
            (is (= [0 21 1 "pre"] (get-product-release versions :mobile)))
            (is (= [0 1 0 "20200301.2"] (get-product-release versions :npdc)))
            (is (= [0 0 0 nil] (get-product-release versions :kiosk)))))
        (testing "Test to derive the new version on branch B"
          (with-redefs [clci.git/commits-on-branch-since (fn [_] petshop/get-git-commits-on-branch-b)]
            (let [versions (calculate-products-version petshop/repo)]
              (is (= [0 7 1 nil] (get-product-release versions :storefront)))
              (is (= [0 5 0] (get-product-release versions :shop-backend)))
              (is (= [0 21 1 "pre"] (get-product-release versions :mobile)))
              (is (= [0 1 0 "20200301.2"] (get-product-release versions :npdc)))
              (is (= [0 0 0 nil] (get-product-release versions :kiosk))))))))))

(ns clci.test-data
  "This module holds datasets used for testing.
   Only for data used by more than one test module!
   
   !!! info
   
       Please always indicate which datesets can and should be used together.
       This module provides the _COMBINERS_ section for this purpose."
  (:require
    [clci.test-utils :refer [commit-hash-re rfc3339-datetime-re commit-author-re]]
    [clojure.spec.gen.alpha :as gen]
    [miner.strgen :as sg]))


;;
;; REPO ;;
;;

(def product-dataset-1
  "Fragment with the products of a repo configuration. 
   Is a repo with multiple products."
  [{:root "pwa/" :key :pwa :release-prefix "pwa" :version "0.0.0"}
   {:root "backend/" :key :backend :release-prefix "kuchen" :version "0.0.0"}
   {:root "common" :key :common :release-prefix "common" :version "0.3.0"}])


(def product-dataset-2
  "Fragment with the products of a repo configuration. Is a single product repo."
  [{:root "" :key :app :release-prefix "app" :version "1.41.2-alpha"}])


(def repo-dataset-1
  "A full repo config dataset. 
   - uses github as SCM
   - has multiple products"
  {:scm
   {:type 		:git
    :url 			"git@github.com:ClockworksIO/clci.git"
    :provider {:name :github
               :repo "clci"
               :owner "ClockworksIO"}}
   :products product-dataset-1})


(def repo-dataset-2
  "A full repo config dataset. 
   - uses github as SCM
   - has a single product"
  {:scm
   {:type 		:git
    :url 			"git@github.com:ClockworksIO/clci.git"
    :provider {:name :github
               :repo "clci"
               :owner "ClockworksIO"}}
   :products product-dataset-2})



;;
;; COMMITS ;;
;;

(def raw-commits-dataset-1
  "Example commits, oldest to newest, for a repo with multiple products.
   Only contains the raw commits w/o any amendments."
  [{:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "chore: inital commit",
    :body ""
    :files
    ["README.md"
     ".gitignore"
     "LICENSE"]}
   ;; 1
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: adding support for green lightsabers",
    :body ""
    :files
    ["pwa/src/core.cljs"
     "pwa/Readme.md"
     "pwa/deps.edn"]}
   ;; 2
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: adding support for pink lightsabers",
    :body ""
    :files
    ["pwa/src/core.cljs"
     "pwa/src/colors.cljs"]}
   ;; 3
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: support blasters in backend",
    :body ""
    :files
    ["backend/src/core.clj"
     "backend/.gitignore"]}
   ;; 4
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "ci: create the build pipeline",
    :body ""
    :files
    [".github/workflows/ci.yaml"
     "docs/index.md"
     ".gitignore"]}
   ;; 5
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "fix: remove a bug",
    :body ""
    :files
    ["backend/src/core.clj"
     "pwa/src/core.cljs"
     ".gitignore"
     "docs/index.md"
     "docs/assets/image.png"]}
   ;; 5
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "fix: remove a bug",
    :body ""
    :files
    ["backend/src/core.clj"
     "pwa/src/core.cljs"
     ".gitignore"
     "docs/index.md"
     "docs/assets/image.png"]}])


(def raw-commits-dataset-2
  "Example commits, oldest to newest, for a repo with a single product."
  [{:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "chore: inital commit",
    :body ""
    :files
    ["README.md"
     ".gitignore"
     "LICENSE"]}
   ;; 1
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "docs: prepare docs",
    :body "BREAKING CHANGE: this commit will break everything"
    :files
    ["mkdocs.yml"
     "docs/index.md"
     "docs/assets/logo.png"]}
   ;; 2
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: implement awesome feature #EXAMPLE-123",
    :body ""
    :files
    ["src/example/core.clj"
     "docs/index.md"
     "docs/assets/logo.png"]}
   ;; 3
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "feat: implement another feature #EXAMPLE-124",
    :body ""
    :files
    ["src/example/utils.clj"
     "assets/img.png"]}
   ;; 4
   {:hash (gen/generate (sg/string-generator commit-hash-re)),
    :date (gen/generate (sg/string-generator rfc3339-datetime-re)),
    :author (gen/generate (sg/string-generator commit-author-re)),
    :subject "fix: implement another feature #EXAMPLE-124",
    :body ""
    :files
    ["src/example/utils.clj"
     "assets/img.png"]}])


;;
;; COMBINERS ;;
;;


(comment 
  "A Dataset for a repo with multiple products.
   Can be used i.e. to test releases."
  repo-dataset-1
  raw-commits-dataset-1
  )


(comment 
  "A Dataset for a repo with a single product.
   Can be used i.e. to test releases."
  repo-dataset-2
  raw-commits-dataset-2
  )

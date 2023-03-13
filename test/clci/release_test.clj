(ns clci.release-test
  "This module provides tests for release tool."
  (:require
    [clci.release :as rel]
    [clci.repo :as rp]
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.test :refer [deftest testing is]]
    [miner.strgen :as sg]))


(defn in?
  "true if coll contains elm."
  [coll elm]
  (some #(= elm %) coll))


(defn not-in?
  "true if coll does not contain elm."
  [coll elm]
  (not (in? coll elm)))


(def commit-hash-re #"[0-9a-f]{5,40}")


(s/def ::commit-hash
  (s/spec
    (s/and string? #(re-matches commit-hash-re %))
    :gen #(sg/string-generator commit-hash-re)))


(def rfc3339-datetime-re #"(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2}(\.\d+)?(Z|([\+-]\d{2}:\d{2}))?)")


(s/def ::commit-date
  (s/spec (s/and string? #(re-matches rfc3339-datetime-re %))
          :gen #(sg/string-generator rfc3339-datetime-re)))


(def commit-author-re #"[0-9a-fA-Z]{5,35}@[0-9a-fA-Z]{5,35}\.[a-z]{2,5}")


(s/def ::commit-author
  (s/spec (s/and string? #(re-matches commit-author-re %))
          :gen #(sg/string-generator commit-author-re)))


(def projects-example-many
  "Example repo.edn project configuration, for a repo with multiple projects."
  [{:root "pwa/" :key :pwa :release-prefix "pwa" :version "0.0.0"}
   {:root "backend/" :key :backend :release-prefix "kuchen" :version "0.0.0"}
   {:root "common" :key :common :release-prefix "common" :version "0.3.0"}])


(def example-commits-many-projects
  "Example commits, oldest to newest, for a repo with multiple projects."
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


(def fake-latest-releases-many
  "Fake latest releases for a repo with multiple projects."
  [{:key    :pwa
    :commit (get-in example-commits-many-projects [0 :hash])
    :tag     "0.0.0"
    :name    "0.0.0"}
   {:key    :backend
    :commit nil
    :tag     "0.0.0"
    :name    "0.0.0"}
   {:key    :common
    :commit (get-in example-commits-many-projects [0 :hash])
    :tag     "0.0.0"
    :name    "0.3.0"}])


(comment
  "Expected versions using the many-projects example:"
  {:pwa     "0.2.1"
   :backend "0.1.1"
   :common  "0.3.0"})


(def projects-example-single
  "Example repo.edn project configuration for a single project repo."
  [{:root "" :key :app :release-prefix "app" :version "1.41.2-alpha"}])


(def example-commits-single-project
  "Example commits, oldest to newest, for a repo with a single project."
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


(def fake-latest-releases-single
  "Fake latest releases for a repo with a single project."
  [{:key    :app
    :commit (get-in example-commits-single-project [0 :hash])
    :tag     "0.0.0"
    :name    "0.0.0"}])


(comment
  "Expected versions using the single-project example:"
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


(deftest affected-projects-many-projects
  (testing "Testing helper function which projects are affected by a commit - many projects."
    (let [amended-commit-log (rel/amend-commit-log example-commits-many-projects)]
      (is (let [result (rel/affected-projects (nth amended-commit-log 0) projects-example-many)]
            (empty? result)))
      (is (let [result (rel/affected-projects (nth amended-commit-log 1) projects-example-many)]
            (in? result '(:pwa :minor))))
      (is (let [result (rel/affected-projects (nth amended-commit-log 2) projects-example-many)]
            (and
              (in? result '(:pwa :minor))
              (not-in? result '(:backend :minor)))))
      (is (let [result (rel/affected-projects (nth amended-commit-log 3) projects-example-many)]
            (and
              (in? result '(:backend :minor))
              (not-in? result '(:pwa :minor)))))
      (is (let [result (rel/affected-projects (nth amended-commit-log 4) projects-example-many)]
            (empty? result)))
      (is (let [result (rel/affected-projects (nth amended-commit-log 5) projects-example-many)]
            (and
              (in? result '(:pwa :patch))
              (in? result '(:backend :patch))))))))


(deftest derive-versions-many-projects
  (testing "Testing to derive the current versions of app projects based on the commit log - many projects."
    (let [derived-versions (rel/derive-current-commit-all-versions-impl
                             (rel/amend-commit-log example-commits-many-projects)
                             projects-example-many)]
      (is (= "0.2.1" (:pwa derived-versions)))
      (is (= "0.1.1" (:backend derived-versions)))
      (is (= "0.3.0" (:common derived-versions))))))



(deftest commit-version-increment-single-project
  (testing "Testing helper function how a commit increments a version - single project."
    (let [amended-commit-log (rel/amend-commit-log example-commits-single-project)]
      (is (nil? (rel/derive-version-increment (-> amended-commit-log (nth 0) :ast))))
      (is (=
            (rel/derive-version-increment (-> amended-commit-log (nth 1) :ast))
            :major))
      (is (= (rel/derive-version-increment (-> amended-commit-log (nth 2) :ast)) :minor))
      (is (let [result (rel/derive-version-increment (-> amended-commit-log (nth 3) :ast))]
            (= result :minor)))
      (is (let [result (rel/derive-version-increment (-> amended-commit-log (nth 4) :ast))]
            (= result :patch))))))


(deftest derive-versions-single-project
  (testing "Testing to derive the current versions of app projects based on the commit log - single project."
    (let [derived-version (rel/derive-current-commit-version-single-project-impl
                            (rel/amend-commit-log example-commits-single-project)
                            (first projects-example-single))]
      (is (= "2.2.1" (:app derived-version))))))


(deftest new-release-required?
  (testing "Testing if a new release is required based on the derived version and latest release."
    (let [derived-versions (rel/derive-current-commit-all-versions-impl
                             (rel/amend-commit-log example-commits-many-projects)
                             projects-example-many)
          grouped-releases (rel/group-gh-releases-by-prefix gh-latest-releases-example-resp)
          mk-fake-project  (fn [version] {:version version})]
      (is (rel/new-release-required?
            (mk-fake-project (get derived-versions :pwa))
            {:version (get-in grouped-releases ["pwa" :version])}))
      (is (rel/new-release-required?
            (mk-fake-project (get derived-versions :backend))
            {:version (get-in grouped-releases ["kuchen" :version])}))
      (is (not
            (rel/new-release-required?
              (mk-fake-project (get derived-versions :common))
              {:version (get-in grouped-releases ["common" :version])}))))))


(deftest prepare-new-releases
  (testing "Testing to derive which releases should be created for the repo based on changes."
    (let [projects              (-> projects-example-many)
          fake-releases         (rel/group-gh-releases-by-prefix gh-latest-releases-example-resp)
          derived-versions      (rel/derive-current-commit-all-versions-impl
                                  (rel/amend-commit-log example-commits-many-projects)
                                  projects-example-many)
          fake-repo             {:projects (map (fn [p]
                                                  (assoc p :version (get derived-versions (:key p))))
                                                projects)}
          result                (into (hash-map) (rel/prepare-new-releases-impl fake-repo fake-releases))]

      (is (contains? result "pwa"))
      (is (= "0.2.1" (get-in result ["pwa" :version])))
      (is (contains? result "kuchen"))
      (is (= "0.1.1" (get-in result ["kuchen" :version])))
      (is (not (contains? result "common"))))))

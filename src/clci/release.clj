(ns clci.release
  (:require
    [babashka.fs :as fs]
    [clci.changelog :as chl]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.github :as gh]
    [clci.repo :as rp]
    [clci.semver :as sv]
    [clci.util.core :refer [str-split-last map-on-map-values any]]
    [clci.util.error :refer [fail! failure? fail-> fail->>]]
    [clojure.spec.alpha :as spec]
    [clojure.string :as str]
    [miner.strgen :as sg]))



(def commit-hash-re #"[0-9a-f]{5,40}")


(spec/def ::hash
  (spec/spec
    (spec/and string? #(re-matches commit-hash-re %))
    :gen #(sg/string-generator commit-hash-re)))


(spec/def ::version #(re-matches (re-pattern sv/semver-regex-pattern) %))
(spec/def ::name string?) ; TODO: should follow the `<prefix> - <semver>` format
(spec/def ::tag string?) ; TODO: should follow the `<prefix> - <semver>` format
(spec/def ::draft? boolean?)
(spec/def ::pre-release? boolean?)
(spec/def ::commit (spec/keys :req-un [::hash]))

(spec/def ::release (spec/keys :req-un [::version ::commit ::name ::tag ::draft? ::pre-release?]))



(defn release-prefix
  "Get the release prefix of the specified `product`."
  [product]
  (str (:release-prefix product)  "-"))


(defn inc-version
  "Increment the version.
   Takes a version as vector of integers `[major minor patch]` and the version increment `v-inc`
   which must be one of `#{:major :minor :patch}`."
  [[major minor patch _] v-incr]
  (case v-incr
    :major [(inc major) 0 0]
    :minor [major (inc minor) 0]
    :patch [major minor (inc patch)]
    [major minor patch]))


(defn derive-version-increment
  "Derive the version increment by the commit message.
  Tages the `ast` of a commit message (following the Conventional Commit Specification)
  and returns one of `:major, :minor or :patch` depending on the type of change (according
  to the Conventional Commit conventions)."
  [ast]
  (cond
    (= (cc/get-type ast) "feat") :minor
    (= (cc/get-type ast) "fix")  :patch
    (cc/is-breaking? ast) :major
    :else  nil))


(defn amend-commit-log
  "Takes a full `commit-log` as produced by `clci.git/commits-on-branch-since` and
   removes all commits not following the CC specs and add the the parsed ast of the
   commits subject to the log entry. Return the purified commit-log."
  [commit-log]
  (->> commit-log
       (map #(assoc % :ast (cc/msg->ast (str (:subject %) "\n\n" (:body %)))))
       (remove #(some? (get-in % [:ast :pod.babashka.instaparse/failure])))))


(defn new-release-required?
  "Predicate to test if the given `product` requires a new release.
   Takes the latest release of the product `latest-release` and compares
   version numbers (SemVer)."
  [product latest-release]
  (sv/newer? (:version product) (:version latest-release)))


;; TODO: TEST THIS WITH LATEST CHANGES!!!
(defn identify-release-prefix
  "Takes the name of a release and extracts the release prefix.
   The `name` of the release must be a string following the SemVer specification."
  [name]
  (let [parts (str-split-last #"-" name)]
    (cond
      (and (= 1 (count parts)) (sv/valid-semver? (first parts))) :no-valid-prefix
      (and (= 2 (count parts)) (sv/valid-semver? (get parts 1))) (first parts)
      :else :no-valid-prefix)))


;; TODO: TEST THIS WITH LATEST CHANGES!!!
(defn release-name->version
  "Get the version from a `release-name`. Expects the release-name to follow the
   <release-prefix>-<semver> schema!"
  [release-name]
  (let [parts (str/split release-name #"-")]
    (cond
      ;; no release prefix found
      (and (= (count parts) 1) (sv/valid-semver? (first parts))) (first parts)
      ;; found a release with prefix, does not use snapshot extension
      (and (> (count parts) 1) (sv/valid-semver? (last parts))) (last parts)
      ;; might be a valid release with prefix and a semver snapshot extension or invalid
      :else nil)))


(defn group-gh-releases-by-prefix
  "Group all releases by their prefix.
   Takes a collection of `releases` as received from the GH API and returns a map
   where the keys are the release prefix and the values are each collections of
   releases ordered with the latest release as first item in the collections."
  [releases]
  (as-> releases $
        (filter #(and (false? (:prerelease %)) (false? (:draft %))) $)
        (group-by (fn [r] (identify-release-prefix (:name r))) $)
        (map-on-map-values $ (fn [releases] (reverse (sort-by :published_at releases))))))


(defn gh-release->release
  "Takes a `gh-release` map as returned from the GH API and a `gh-tag` map as returned from the GH API
   and returns a release map in the format used by clci."
  [gh-release gh-tag]
  {:commit        {:hash (:sha gh-tag)}
   :tag           (or (:tag_name gh-release) "0.0.0")
   :name          (or (:name gh-release) "0.0.0")
   :version       (or (release-name->version (:name gh-release)) "0.0.0")
   :draft?        (:draft gh-release)
   :pre-release?  (:prerelease gh-release)
   :published     (:published_at gh-release)})


(defn reduce-to-last-release
  "Takes a map of `grouped-releases` where the value to each (release-prefix) key
   is a collection of releases ordered latest to oldest. Reduces the map to only
   have the latest release for each product.
   Returns a map in the format `<prefix> : <latest-release>`"
  [grouped-releases]
  (map-on-map-values
    grouped-releases
    (fn [releases] (first releases))))


(defn- get-latest-releases
  "Get the latest release for each product.
   Takes the `repo` config and returns a map where the keys are the prefix of the release (string) and
   The value is the latest release with this prefix. Releases with no or invalid prefix are grouped
   under the `:no-valid-prefix` key.
   **Warning**: Only supports Github Releases at this time!"
  [repo]
  (let [scm-owner       (get-in repo [:scm :provider :owner])
        scm-repo        (get-in repo [:scm :provider :repo])]
    (case (get-in repo [:scm :provider :name])
      ;; scm == github
      :github (let [all-releases    (gh/list-releases scm-owner scm-repo)]
                (-> all-releases
                    group-gh-releases-by-prefix
                    reduce-to-last-release
                    (map-on-map-values (fn [gh-release] (gh-release->release gh-release (gh/get-tag scm-owner scm-repo (:tag_name gh-release)))))))
      ;; otherwise - not supported
      (ex-info "Only Github is supported as SCM provider!" {}))))



(defn get-latest-release
  "Get the latest release of the product.
   Takes the `repo` configuration and optionally a `product-key` to get the latest release
   for a specific product. The product-key is required if more than one project is present!
   
   !!!! DEPRECATED"
  ([repo]
   (if (rp/single-product?)
     (get-latest-release repo (get-in repo [:products 0]))
     (ex-info "Getting the latest release requires a specific product key if the repo hast more than one product!" {})))
  ;; ([repo]
  ;;  (when-not (rp/single-product?)
  ;;    (ex-info "Getting the latest release requires a specific product key if the repo hast more than one product!" {}))
  ;;  (case (get-in repo [:scm :provider :name])
  ;;    ;; scm == github
  ;;    :github (let [repo-name  (get-in repo [:scm :provider :repo])
  ;;                  owner      (get-in repo [:scm :provider :owner])
  ;;                  product    (get-in repo [:products 0])
  ;;                  ;release 	(gh/get-latest-release owner repo-name)
  ;;                  last-release-name  (git/get-product-latest-release-name product)
  ;;                  release   (gh/last-release-name owner repo last-release-name)
  ;;                  tag       (gh/get-tag owner repo-name (:tag_name release))]
  ;;              (gh-release->release release tag))
  ;;    (ex-info "Only Github is supported as SCM provider!" {})))
  ([repo product]
   (case (get-in repo [:scm :provider :name])
     :github (let [repo-name  (get-in repo [:scm :provider :repo])
                   owner      (get-in repo [:scm :provider :owner])
                   ;; product    (get-in repo [:products 0])
                   ;; release   (gh/get-latest-release owner repo-name)
                   last-release-name  (git/get-product-latest-release-name product)
                   release   (gh/get-release-by-tag-name owner repo last-release-name)
                   tag       (gh/get-tag owner repo-name (:tag_name release))]
               (gh-release->release release tag))
     ;; unrecognized scm provider
     (ex-info "Only Github is supported as SCM provider!" {}))))


(defn prepare-new-releases-impl
  "Calculate which products need a new release and prepare all data required to create
   a new release."
  [repo latest-releases]
  (as-> latest-releases $
        (filter
          (fn [[prefix release]]
            (when-let  [product (rp/get-product-by-release-prefix prefix repo)]
              (new-release-required? product release)))
          $)
        (keys $)
        (map (fn [prefix] [prefix (rp/get-product-by-release-prefix prefix repo)]) $)
        (into (hash-map) $))
  ;; TODO: use new version info not the old release info!
  )


(defn create-releases
  "Create all releases.
   Identifies the products that where changed (new version) since the last release found on
   the SCM/Release platform and creates new releases for each of them."
  []
  (let [repo 								(rp/read-repo)
        all-latest-releases (dissoc (get-latest-releases repo) :no-valid-prefix)
        to-be-released 			(prepare-new-releases-impl repo all-latest-releases)]
    ;; scm == github
    (case (get-in repo [:scm :provider :name])
      :github (let [repo-name (get-in repo [:scm :provider :repo])
                    owner     (get-in repo [:scm :provider :owner])]
                (doseq [[_ product] to-be-released]
                  (gh/create-release {:owner owner :repo repo-name :tag (str (release-prefix product) (:version product)) :draft false :pre-release false})
                  ;; TODO: add options to set as draft or pre-release
                  )
                (ex-info "Only Github is supported as SCM provider for Releases!" {})))))



(defn derive-current-commit-version-single-product-impl
  "Implementation of `clci.tools.release/derive-current-commit-version-single-product`.
   Takes a collection `amended-commit-log` with the amended commit history since the last
   release and the `product` from the repositorie's repo.edn configuration."
  [amended-commit-log product]
  (let [version-increments (->> amended-commit-log (map (comp derive-version-increment :ast)) (remove nil?))]
    (->> version-increments
         (reduce (fn [acc v-incr]
                   (inc-version acc v-incr))
                 (sv/version-str->vec (:version product)))
         (sv/version-vec->str))))


(defn derive-current-commit-version-single-product
  "Derive the version of the current commit for a single product repository."
  []
  (let [repo                 (rp/read-repo)
        ;; get the last release using the gh api
        latest-release       (get-latest-release repo)
        ;; get a git log of all commits since the latest release and amend it with the commit
        ;; message ast
        amended-commit-log   (amend-commit-log (git/commits-on-branch-since {:since (get-in latest-release [:commit :hash])}))
        ;; get the product, the version information is required
        product              (first (rp/get-products))]
    ;; using all information we collected we can now calculate the new version
    (derive-current-commit-version-single-product-impl amended-commit-log product)))


(defn affected-products
  "Tests which of the products are affected by the commit.
   Does so by checking if the changes happened in the root of each product.
   Takes an amended `commit` and the `products` of the repo. "
  [commit products]
  (->> products
       (map (fn [prod]
              (when (any (fn [f] (fs/starts-with? f (:root prod))) (:files commit))
                [(:key prod) (derive-version-increment (:ast commit))])))
       (remove nil?)))


(defn derive-current-commit-all-versions-impl
  "Implementation to caculate the version of all products based on the commit history.
   Takes a collection `amended-commit-log` with the amended commit history since the last
   release, the `products` from the repositories repo.edn configuration."
  [amended-commit-log products]
  (let [version-increments  (map (fn [commit] (affected-products commit products)) amended-commit-log)
        with-update         (fn [acc increments]
                              (reduce
                                (fn [a [key v-inc]] (assoc a key (inc-version (get a key) v-inc)))
                                acc increments))]
    (->
      (loop [head (first version-increments)
             tail (rest version-increments)
             acc  (into (sorted-map) (map (fn [p] [(:key p) (sv/version-str->vec (:version p))]) products))]
        (if (empty? tail)
          acc
          (recur (first tail) (rest tail) (with-update acc head))))
      (map-on-map-values sv/version-vec->str))))



(defn derive-current-commit-all-versions
  "Caculate the version of all products based on the commit history."
  []
  (let [repo 									(rp/read-repo)
        ;; get the last release using the gh api, that is the latest of any product releases!
        latest-release        (get-latest-release repo)
        ;; we need the version as an easy to manipulate datastructure, not just as string
        ;; last-release-version  (tf-release-version latest-release)
        commit-log            (amend-commit-log (git/commits-on-branch-since {:since (:commit latest-release)}))
        products              (rp/get-products)]
    (derive-current-commit-all-versions-impl commit-log products)))


(defn derive-current-commit-version
  "Derive the version of the current codebase.
  Uses the latest release that exists and the git log which must follow the conventional commits
  specification. Depending on the type of the commit the new version will be calculated
  following the semantic versioning specs.
  Takes into account if the repo has a single product or many."
  []
  (if (rp/single-product?)
    (derive-current-commit-version-single-product)
    (derive-current-commit-all-versions)))


;; Get the latest release of a product
;; Takes the `product` and the `scm` information to dispatch the correct
;; scm provider implementation
(defmulti get-product-latest-release (fn [_ scm] (get-in scm [:provider :name])))


;; Get the latest release for SCM provider github
;; !!! warning
;;
;;     Only works with a single product release or a combined release because the
;;     github API can only provide a single release marked as _latest_.
(defmethod get-product-latest-release :github [product scm]
  (let [repo-name (get-in scm [:provider :repo])
        owner     (get-in scm [:provider :owner])
        ;; release   (gh/get-latest-release owner repo-name)
        ;; tag       (gh/get-tag owner repo-name (:tag_name release))
        last-release-name  (git/get-product-latest-release-name product)
        release   (gh/get-release-by-tag-name owner repo-name last-release-name)
        tag       (gh/get-tag owner repo-name (:tag_name release))]
    (println "last release name: " last-release-name)
    (println "last release: " (gh-release->release release tag))
    (gh-release->release release tag)))


;; Only github is supported at this time.
;; Support for other SCM provider may follow in the future. Do you want to support
;; another SCM provider? I'd love to receive a PR for this :)
(defmethod get-product-latest-release :default [_ scm] (ex-info "Invalid SCM provider!" {:scm scm}))


(defn- prepare-release-single-product
  "Prepare a release - only for repos with a single product.
   "
  [set-version? update-changelog?]
  (let [repo                 (rp/read-repo)
        product              (get-in repo [:products 0])
        latest-release       (get-product-latest-release product (:scm repo))
        amended-commit-log   (amend-commit-log (git/commits-on-branch-since {:since (get-in latest-release [:commit :hash])}))
        derived-version      (derive-current-commit-version-single-product-impl amended-commit-log product)]
    (when set-version?
      (rp/update-product-version derived-version (:key product)))
    (when update-changelog?
      (chl/update-changelog! amended-commit-log))
    {:version derived-version
     :amended-log amended-commit-log}))


(defn- prepare-combined-release
  "Prepare a release - for repos with multiple products but combined release tracking."
  [_ _]
  (ex-info "Not implemented yet!" {}))


(defn- prepare-release-all-products
  "Prepare a release for all products.
   
   NOT IMPLEMENTED YET! 
   
   Please see #111 for more information why this is not yet implemented."
  [_ _]
  (ex-info "Not implemented yet! See #111." {}))


(defn prepare-release!
  "Prepare a new release.
   Calculates the new versions of the products and updates the repo.edn configuration
   with the new version numbers.
   Takes the following arguments:
   | key                 | default | description                  |
   | --------------------|---------|------------------------------|
   | `update-version?`   | true    | When true, update the versions of the products in the repo.edn file.
   | `update-changelog?` | false   | When true, update the Changelog file(s)."
  [{:keys [update-version? update-changelog?] :or {update-version? true update-changelog? false}}]
  (cond
    (rp/single-product?)              (prepare-release-single-product update-version? update-changelog?)
    (rp/combined-release-tracking?)   (prepare-combined-release update-version? update-changelog?)
    :else                             (prepare-release-all-products update-version? update-changelog?)))


(defn create-release!
  ""
  [])


(comment
  "Next steps:
   [x] move function `get-product-latest-release` from git module here
   [x] create function to fetch specific gh release")


(prepare-release! {:update-version? true :update-changelog? true})

(ns clci.release
  (:require
    [babashka.fs :as fs]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.github :as gh]
    [clci.repo :as rp]
    [clci.semver :as sv]
    [clci.util.core :refer [any join-paths map-on-map-values]]
    [clojure.core :as c]
    [clojure.spec.alpha :as spec]
    [clojure.string :as str]
    [config :as config]
    [miner.strgen :as sg]))


(comment
  ;;; Define basic constraints for the examples used in this module
  ;;; The examples use the clci repo itself as a basw
  
  ;; Define some bricks
  (def clci-bricks [])

  ;; Define some products
  (def clci-product
    {:root ".",
     :version "0.21.0",
     :key :clci,
     :release-prefix "clci",
     :type :other})

  ;; Define the overall repo config for the examle
  (def clci-repo
    {:scm   {:type :git,
             :url "git@github.com:ClockworksIO/clci.git",
             :provider {:name :github, :repo "clci", :owner "ClockworksIO"}}
     :products [clci-product]
     :bricks clci-bricks})
)


(def commit-hash-re
  "RegEx for a commit hash (a sha2 hash as hex string)"
  #"[0-9a-f]{5,40}")


(spec/def ::hash
  (spec/spec
    (spec/and string? #(re-matches commit-hash-re %))
    :gen #(sg/string-generator commit-hash-re)))


(spec/def :tag/name string?)  ; TODO: should follow the `<prefix> - <semver>` format
(spec/def :tag/version string?) ; TODO: should follow the `<semver>` format
(spec/def :tag/commit-sha string?) ; TODO: make this a proper sha spec
(spec/def :tag/ref string?) ; TODO: make this a proper git tag ref spec
(spec/def :tag/release? boolean?)



(spec/def ::tag
  (spec/keys
    :req-un [:tag/name :tag/commit-sha :tag/ref]
    :opt-un [:tag/version :tag/release?]))


(spec/def :release/version #(re-matches sv/semver-re %))
(spec/def :release/name string?) ; TODO: should follow the `<prefix> - <semver>` format
(spec/def :release/tag-name string?) ; TODO: should follow the `<prefix> - <semver>` format !DEPRECATED!
(spec/def :release/tag ::tag)
(spec/def :release/draft? boolean?)
(spec/def :release/pre-release? boolean?)
(spec/def :release/commit (spec/keys :req-un [::hash]))
(spec/def :release/assets any?)


(spec/def ::release
  (spec/keys
    :req-un [:release/version :release/commit :release/name :release/tag
             :release/draft? :release/pre-release? :release/assets]
    :opt-un [:release/tag-name]))


(def product-release-prefix-re
  "Regex for the release prefix of a product."
  #"[a-zA-Z0-9\-_]{3,}\-")


(def brick-version-tag-prefix-re
  "Regex for the version tag prefix of a brick."
  "brick/[a-zA-Z0-9\\-_]{3,}\\-")


(def product-release-tag-re
  "Regex for a product release tag (prefix and semver part)."
  (re-pattern (str product-release-prefix-re sv/semver-re)))


(def brick-version-tag-re
  "Regex for a brick release tag (prefix and semver part)."
  (re-pattern (str brick-version-tag-prefix-re sv/semver-re)))


(defn release-prefix
  "Get the release prefix of the specified `product`."
  [product]
  (str (or (:release-prefix product) (:name product))))


(defn brick-release-prefix
  "Get the release prefix of the specified `brick`."
  [brick]
  (str "brick/" (name (:key brick))))


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


(defn transform-commit-log
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


(defn- remove-from-end
  "Remove the last character from string `s` if it equals `end`."
  [s end]
  (when-not (nil? s)
    (if (str/ends-with? s end)
      (subs s 0 (- (count s) (count end)))
      s)))


(defn tag->release-prefix
  "Get the release prefix from a `tag` string.
   **Example**:
   ```clojure
   (release-tag->release-prefix \"foo-bar-1.2.3-20240524.1\")
   ; -> foo-bar
   ```"
  [tag]
  (remove-from-end (re-find (re-pattern product-release-prefix-re) tag) "-"))


(defn tag->brick-version-prefix
  "Get the full brick version prefix from `tag` string.
   **Example**:
   ```clojure
   (tag->brick-version-prefix \"brick/foo-bar-1.2.3-20240524.1\")
   ; -> brick/foo-bar
   ```"
  [tag]
  (remove-from-end (re-find (re-pattern brick-version-tag-prefix-re) tag) "-"))


(defn release-tag->semver-tag
  "Get the semver part from a `tag` string.
   **Example**:
   ```clojure
   (release-tag->semver-tag \"foo-bar-1.2.3-20240524.1\")
   ; -> 1.2.3-20240524.1
   ```"
  [tag]
  (first (re-find sv/semver-re tag)))


(defn release-tag->maj-min-patch
  "Get the major-minor-patch part from a `tag` string.
   **Example**:
   ```clojure
   (release-tag->maj-min-patch \"foo-bar-1.2.3-20240524.1\")
   ; -> 1.2.3
   ```"
  [tag]
  (first (re-find (re-pattern sv/semver-m-m-p-tag) tag)))


(defn- file-in-directory?
  "Predicate to test if the given `file` path is inside of the given
   directory path given as `dir`."
  [file dir]
  (let [dir' (if (= dir ".") "" dir)]
    (fs/starts-with? (str (fs/absolutize file)) (str (fs/absolutize dir')))))


(defn commit-affects-product?
  [commit product]
  (any (fn [f] (file-in-directory? f (:root product))) (:files commit)))


(defn commit-affects-brick?
  [commit brick]
  (any (fn [f] (file-in-directory? f (join-paths rp/brick-dir (name (:key brick))))) (:files commit)))


(defn product-release-tag?
  "Predicate to test if the given `tag` string is a valid product release tag."
  [tag]
  (some? (seq (re-matches product-release-tag-re tag))))


(defn brick-version-tag?
  "Predicate to test if the given `tag` string is a valid brick version tag."
  [tag]
  (some? (seq (re-matches brick-version-tag-re tag))))


(defn filter-product-release-tags
  "Filters only the tags that point to a release.
   Takes a `product` following the `:clci.repo/product` spec and a list of `tags`
   where each tag follows the form as returned from `clci.github/tag-refs->tags`."
  [product tags]
  (->> tags
       (filter (fn [tag] (product-release-tag? (:name tag))))
       (filter (fn [tag] (= (release-prefix product) (tag->release-prefix (:name tag)))))))


(defn filter-brick-version-tags
  "Filters only the tags that point to a brick version.
   Takes a `brick` following the `:clci.repo/brick` spec and a list of `tags`
   where each tag follows the form as returned from `clci.github/tag-refs->tags`."
  [brick tags]
  (->> tags
       (filter (fn [tag] (brick-version-tag? (:name tag))))
       (filter (fn [tag] (= (brick-release-prefix brick) (tag->brick-version-prefix (:name tag)))))))


(defn find-product-latest-release-tag
  "Find the latest release tag for a product.
   Takes the `product` as a map following `:clci.repo/product` spec and a list
   of tags. Identifies the latest release of the product by searching for valid
   release tags for the product and returns the matching release tag with the
   latest version information following semver.
   **Example**:
   ```clojure
   (find-product-latest-release-tag {...} '(...)) ; -> \"1.3.5\"
   ```"
  [product tags]
  (->> tags
       (filter-product-release-tags product)
       (map (fn [tag] (assoc tag :version (release-tag->semver-tag (:name tag)))))
       (sort-by :version (fn [a b] (sv/newer? a b)))
       first))


(defn find-all-products-latest-release-tag
  "Find the latest release tag for all products.
   Takes avector of `products` where each element is a map following the
   `:clci.repo/product` spec and a list of tags. 
   Identifies the latest release of each product by searching for valid
   release tags for the product and returns a map where each key is a product
   key and the value is the latest version.
   **Example**:
   ```clojure
   (find-all-products-latest-release-tag {...} '(...)) 
   ; -> 
   ; {:productA \"1.3.5\"
   ;  :productB \"3.1.4\"}
   ```"
  [products tags]
  (->> products
       (mapv (fn [product]
               [(:key product) (find-product-latest-release-tag product tags)]))
       (into {})))


(defn get-all-brick-tags
  "Get all tags that point to a brick version.
   Takes the `repo` following the spec of `:clci/repo` and returns a
   list with all tags pointing to a brick version."
  [repo]
  (let [gh-tags             (gh/get-all-tag-refs (get-in repo [:scm :provider :owner]) (get-in repo [:scm :provider :repo]))
        tags                (gh/tag-refs->tags gh-tags)]
    (filter brick-version-tag? tags)))


(defn find-brick-version-tags
  "Find all version tag for a brick.
   Takes the `brick` as a map following `:clci.repo/brick` spec and a list
   of tags. Identifies all tagged versions of the brick by searching for valid
   brick version tags for the brick and returns the matching version tag with the
   latest version information following semver.
   **Example**:
   ```clojure
   (find-brick-latest-version-tag {...} '(...)) ; -> \"1.3.5\"
   ```"
  [brick tags]
  (->> tags
       (filter-brick-version-tags brick)
       (map (fn [tag] (assoc tag :version (release-tag->semver-tag (:name tag)))))
       (sort-by :version (fn [a b] (sv/newer? a b)))))


(defn find-brick-latest-version-tag
  "Find the latest version tag for a brick.
   Takes the `brick` as a map following `:clci.repo/brick` spec and a list
   of tags. Identifies the latest tagged version of the brick by searching for valid
   brick version tags for the brick and returns the matching version tag with the
   latest version information following semver.
   **Example**:
   ```clojure
   (find-brick-latest-version-tag {...} '(...)) ; -> \"1.3.5\"
   ```"
  [brick tags]
  (first (find-brick-version-tags brick tags)))


(defn find-all-bricks-latest-version-tag
  "Find the latest version tags for all bricks.
   Takes avector of `bricks` where each element is a map following the
   `:clci.repo/brick` spec and a list of tags. 
   Identifies the latest tagged version of the brick by searching for valid
   version tags for the brick and returns a map where each key is a brick
   key and the value is the latest version.
   **Example**:
   ```clojure
   (find-all-bricks-latest-version-tag {...} '(...)) 
   ; -> 
   ; {:productA \"1.3.5\"
   ;  :productB \"3.1.4\"}
   ```"
  [bricks tags]
  (->> bricks
       (mapv (fn [brick]
               [(:key brick) (find-brick-latest-version-tag brick tags)]))
       (into {})))


(defn get-release-for-tag
  "Takes the `tag` following the `:clci/tag` spec and the name
   of the github repository owner `gh-owner-name` and the name
   of the github repository `gh-repo-name`.
   Returns a release following the `:clci/release` spec.
   Returns nil if no release was found.
   Makes an API call to github.com."
  [tag gh-owner-name gh-repo-name]
  (try
    (let [gh-release (gh/get-release-by-tag-name gh-owner-name gh-repo-name (:name tag))]
      {:version       (:version tag)
       :name          (:name gh-release)
       :tag           tag
       :tag-name      (:name tag)
       :draft?        (:draft gh-release)
       :pre-release?  (:prerelease gh-release)
       :commit        {:hash (:commit-sha tag)}
       :assets        (:assets gh-release)})
    (catch Exception ex
      nil)))


(comment 
  ; (def clci-bricks [])


  ; (def clci-product
  ;   {:root ".",
  ;    :version "0.21.0",
  ;    :key :clci,
  ;    :release-prefix "clci",
  ;    :type :other})


  ; (def clci-repo
  ;   {:scm   {:type :git,
  ;            :url "git@github.com:ClockworksIO/clci.git",
  ;            :provider {:name :github, :repo "clci", :owner "ClockworksIO"}}
  ;    :products [clci-product]
  ;    :bricks clci-bricks})


  (def gh-tags (gh/get-all-tag-refs "ClockworksIO" "clci"))
  (def tags (gh/tag-refs->tags gh-tags))

  (def latest-brick-versions-tags   (find-all-bricks-latest-version-tag (:bricks clci-repo) tags))
  (def latest-product-version-tags (find-all-products-latest-release-tag (:products clci-repo) tags))
  latest-product-version-tags


  ;; ->
  ;; {:clci {:name "clci-0.21.0",
  ;;         :sha "d66f9bad476d82b6ee254f8f80c5c627d5887615",
  ;;         :ref "refs/tags/clci-0.21.0",
  ;;         :url "https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/clci-0.21.0",
  ;;         :version "0.21.0"}}


  (def release
    (get-release-for-tag
      (:clci latest-product-version-tags)
      (get-in clci-repo [:scm :provider :owner])
      (get-in clci-repo [:scm :provider :repo])))
  
  ; release


; {:version "0.21.0",
;  :name "clci-0.21.0",
;  :tag {:name "clci-0.21.0",
;        :commit-sha "d66f9bad476d82b6ee254f8f80c5c627d5887615",
;        :ref "refs/tags/clci-0.21.0",
;        :url "https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/clci-0.21.0",
;        :version "0.21.0"},
;  :tag-name "clci-0.21.0", :draft? false, :pre-release? false, :commit {:hash "d66f9bad476d82b6ee254f8f80c5c627d5887615"}, :assets []}

)


(defn calculate-brick-version
  "Calculate the (new) version of a brick.
   Takes the `last-tag` of the brick (map following the format of `clci.github/tag-ref->tag`) and
   the `brick` (map following the `:clci.repo/brick` spec).
   Calculates the new version using the semver convention and conventional commits. 
   Commits not following the conventional commit specification are ignored.
   Returns the new version as vector `[major minor patch]`."
  [last-tag brick & {:keys [trunk]}]
  (let [version (sv/version-str->vec (:version brick))
        commits-since-tag (transform-commit-log (git/commits-on-branch-since {:since (:commit-sha last-tag) :branch trunk}))
        increments (->> commits-since-tag
                        (filter (fn [commit] (commit-affects-brick? commit brick)))
                        (remove nil?)
                        reverse
                        (map :ast)
                        (map derive-version-increment))
        version' (reduce inc-version version increments)]
    version'))


(defn calculate-bricks-version
  "Calculate the version for each brick in the repo.
   Takes the `repo` configuration following the `:clci/repo` spec.
   Returns a vector of `[<brick-key> <version>]` where the version is in
   vector form.
   Calling this function will issue a request to the Github API."
  [repo & {:keys [trunk]}]
  (let [bricks              (rp/get-bricks repo)
        gh-tags             (gh/get-all-tag-refs (get-in repo [:scm :provider :owner]) (get-in repo [:scm :provider :repo]))
        tags                (gh/tag-refs->tags gh-tags)
        latest-brick-tags   (find-all-bricks-latest-version-tag bricks tags)
        get-brick-tag       (fn [b-key]
                              (->> latest-brick-tags
                                   (filter
                                     (fn [[key _]] (= key b-key)))
                                   first
                                   second))]
    (mapv
      (fn [brick]
        [(:key brick)
         (calculate-brick-version
           (get-brick-tag (:key brick))
           brick
           {:trunk trunk})])
      bricks)))


(defn get-product-latest-release
  "Get the latest release for a product.
   Takes the `repo` as a map following `:clci/repo` spec and the `product`
   as a map following `:clci.repo/product` spec. Finds the latest release of the
   product using the gh API.
   Returns the latest release as a map following the `:clci.release/release` spec."
  [repo product]
  (let [gh-tags             (gh/get-all-tag-refs (get-in repo [:scm :provider :owner]) (get-in repo [:scm :provider :repo]))
        tags                (gh/tag-refs->tags gh-tags)
        latest-release-tag  (find-product-latest-release-tag product tags)]
    (get-release-for-tag
      latest-release-tag
      (get-in repo [:scm :provider :owner])
      (get-in repo [:scm :provider :repo]))))


(comment
  ;; Get the latest release of clci
  (def clci-latest-release (get-product-latest-release clci-repo clci-product))
  clci-latest-release
  )


(defn get-products-latest-release
  "Get the latest release for many products.
   Takes the `repo` as a map following `:clci/repo` spec and the `products`
   as a vector of maps following `:clci.repo/product` spec. Finds the latest release for each
   product using the gh API.
   Returns a vector of pairs with the key of the product and the latest release of each specific product
   as a map following the `:clci.release/release` spec."
  [repo products]
  (mapv
    (fn [product]
      [(:key product) (get-product-latest-release repo product)])
    products))


(defn calculate-product-version
  "Calculate the (new) version of a product.
   Takes the `last-release` of the product (map following the `:clci.release/release` spec) and
   the `product` (map following the `:clci.repo/product` spec).
   Fetches all commits on the current branch(!) since the commit of `last-release` and calculates
   the new version using the semver convention and conventional commits. Commits not following the
   conventional commit specification are ignored.
   Returns the new version as vector `[major minor patch]`."
  [last-release product & {:keys [trunk]}]
  (let [version (sv/version-str->vec (:version product))
        commits-since-last-release (transform-commit-log
                                     (git/commits-on-branch-since {:since (get-in last-release [:commit :hash]) :branch trunk}))
        increments (->> commits-since-last-release
                        (filter (fn [commit] (commit-affects-product? commit product)))
                        (remove nil?)
                        reverse
                        (map :ast)
                        (map derive-version-increment))
        version' (reduce inc-version version increments)]
    version'))


(comment
  ;; Example:
  (calculate-product-version release clci-product)
)


(defn calculate-products-version
  "Calculate the version for each product in the repo.
   Takes the `repo` configuration following the `:clci/repo` spec.
   Returns a vector of `[<product-key> <version>]` where the version is in
   vector form.
   Calling this function will issue a request to the Github API."
  [repo & {:keys [trunk]}]
  (let [products            (rp/get-products repo)
        product-releases    (get-products-latest-release repo products)
        get-release         (fn [p-key]
                              (->> product-releases
                                   (filter
                                     (fn [[key _]] (= key p-key)))
                                   first
                                   second))]
    (mapv
      (fn [product]
        [(:key product)
         (calculate-product-version
           (get-release (:key product))
           product
           {:trunk trunk})])
      products)))


;; (defn derail
;;   [[result status] f  & args]
;;   (if status
;;     status
;;     (apply f result args)))



(defn release-product!
  "Create a new release for a product.
   Takes the `repo`, the `product` that should be released and a flag
   `pre-release?`. 
   Compares the current version of the product as defined in the repo.edn
   file and compares it with the latest release available through the Github
   API. If the version is newer, then create a new release for the product
   using the Github API.
   Throws an ExceptionInfo if the release could not be created.
   Makes requests to the Githun API!"
  [repo product pre-release?]
  (let [latest-release (get-product-latest-release repo product)
        repo-name (get-in repo [:scm :provider :repo])
        owner     (get-in repo [:scm :provider :owner])]
    (try
      (if (sv/newer? (:version product) (:version latest-release))
        (gh/create-release {:owner owner :repo repo-name :tag (str (release-prefix product) "-" (:version product)) :draft false :pre-release pre-release?})
        :release-for-version-exists)
      (catch Exception ex
        (throw (ex-info "Unable to create release" {:cause :github-api-error :data (ex-data ex)}))))))


(defn release-new-products!
  "Create Releases for all products.
   Takes a flag `pre-release?`."
  [pre-release?]
  (let [repo (rp/read-repo)]
    (doseq [product (rp/get-products)]
      (release-product! repo product pre-release?))))


(comment
  ;; Important: Bricks MUST NEVER depent on other Bricks!
  ;;
  
  ;; Convention:
  ;; [name]-[semver]
  ;; -> [name]-{[<major>.<minor>.<patch>]-[suffix]}
  ;; -> [a-release-prefix]-[1.2.4] or [a-release-prefix]-[1.2.4]-[snapshot-20240501.1]
  
  ; 1. Get a list with all Tags -> [:done]
  ;   - fetch all tags from the GH API using multiple pages
  ; 2. Find the latest Release Tag for each Product -> [:done]
  ;   - using the (derived) release prefix of each product and semver order
  ; 3. Find the latest version Tags for all Bricks -> [:done]
  ;   - using the name of each brick and semver order
  ; 4. Find the Release for each Product-ReleaseTag Pair -> [:done]
  ;   - using the GH API to get a Release for a specific Tag
  
  ; 5. For all Bricks calculate the new Brick version -> [:done]
  ;   1. Find the commits that affect each Brick since its latest version Tag
  ;   2. Calculate the new version of each Brick
  ;   3. Write the new Version of each Brick to the repo.edn file

  
  ; 6. For Each Product -> [:done]
  ;   1. Get the commit history from the latest Release until now -> [:done]
  ;   2. Filter commits that are  -> [:done]
  ;     a. either relevant for the product or
  ;     b. relevant for a Brick used by the Product
  ;   3. Calculate the new version of the Product -> [:done]
  ;     a. For commits that directly affect the product, use the Conventional Commit
  ;        tag and Semantic Versioning to increment the version
  ;     b. For commits that affect NOT the product but a Brick used by the Product,
  ;        use a FIX level version increment
  ;   4. Write the Product Version of each Product to the repo.edn file -> [:done]
  
  ; ... write changelogs
  
  ; 7. Create a commit with the new Changelogs and updated version information
  ;    in the repo.edn file
  ; 8. Tag the commit with
  ;   a. git tags for each Brick that has gotten a new version
  ;   b. git tags for each Product that has gotten a new version
  ; 9. For each Product create a Release using the GH API using the new version tag
  )

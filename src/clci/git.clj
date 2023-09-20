(ns clci.git
  "Various git tooling."
  (:require
    [babashka.process :refer [sh shell]]
    [clci.constants :refer [latest-release-tag-suffix]]
    [clci.semver :refer [semver-regex-pattern release-prefix-regex-pattern]]
    [clojure.string :as str]))


(defn get-tags
  "Get a list of all tags."
  []
  (-> (shell {:out :string} "git tag")
      :out
      str/split-lines))


(defn delete-tag
  "Delte a tag.
   Takes a `tag` to be deleted and an optional `delete-remote?` flag. If the flag is true
   the tag will also be deleted in the remote repository."
  ([tag] (delete-tag tag true))
  ([tag delete-remote?]
   (->
     (shell {:out :string} (format "git tag -d %s" tag)))
   (when delete-remote?
     (shell {:out :string} (format "git push origin :refs/tags/%s" tag)))))


(defn current-commit
  "Get the current commit on the current branch."
  []
  (-> (sh "git" "rev-parse" "HEAD")
      :out
      str/trim))


(defn current-branch-name
  "Get the name of the current branch."
  []
  (->
    (shell {:out :string} "git rev-parse --abbrev-ref HEAD")
    :out))


(defn commits-on-branch-since
  "Get all commits using git cli.
  Reads the git log using the oneline format. Optionally takes
  | key          | description |
  | -------------|-------------|
  | `:branch`    | (optional) The branch to get the commits. Defaults to `master`
  | `:since`     | (optional) Commit SHA since when to get all commits. Defaults to the first commit on the branch.
  | `:with-tags` | (optional) When set, the log will include commits that add a tag. Defaults to `false`.
  "
  ([] (commits-on-branch-since {}))
  ([{:keys [branch since with-tags]
     :or {branch    "master"
          since     nil
          with-tags false}}]
   (let [;; sanitize a single log entry into a map
         ;; used after grouping the log lines into commits
         commit-col->map   (fn [v]
                             {:hash    (get v 0)
                              :date    (get v 1)
                              :author  (get v 2)
                              :subject (get v 3)
                              :body    (get v 4)
                              :files   (get v 5)})
         ;; split entries on linebreak and sanitize whitespace
         splitter (comp (partial into []) #(remove str/blank? %) str/split-lines)
         ;; git shell command used to get the log in the format required
         cmd (str/join
               " "
               ["git log --format=\"%n!-M-!%H%n%ai%n%ae!-S-!%s!-B-!%b!-F-!\""
                "--name-only"
                (when-not with-tags "--decorate-refs-exclude=refs/tags")
                (format "--first-parent %s" branch)
                (when since (format "%s..HEAD" since))])]
     (as-> (shell {:out :string} cmd) $
           (:out $)
           (str/split $ #"!-M-!")
           (remove str/blank? $)
           (map #(str/split % #"(!-S-!)|(!-B-!)|(!-F-!)") $)
           (map (fn [[head subject body changes]]
                  (commit-col->map (apply conj [(splitter head) subject body (splitter changes)])))
                $)))))


(defn staged-files
  "Get a collection of all changed files staged for the next commit."
  []
  (-> (shell {:out :string} "git --no-pager diff --name-only --no-color --cached --diff-filter=ACM")
      :out
      str/split-lines))


(defn fetch-origin!
  "Fetch all changes from git remote 'origin'.
  Returns the output of the fetch as string on success.
  Throws an exception if fetching from remote failed."
  []
  (-> (shell {:out :string} "git fetch origin")
      :out))


(defn derive-tag-name-for-latest-release
  "Get the latest release tag for the given `product`."
  [product]
  (format "%s%s" (:release-prefix product) latest-release-tag-suffix))


(defn get-commit-from-tag
  "Get the commit sha where the given `tag-name` points to."
  [tag-name]
  (-> (shell {:out :string} (format "git rev-list -n 1 tags/%s" tag-name)) :out (str/trim)))


(defn get-all-tags-of-commit
  "Get all tags pointing to a commit.
   Takes the `hash` of the commit and returns a vector with tag names."
  [hash]
  (-> (shell {:out :string} (format "git tag --points-at %s" hash)) :out (str/split-lines)))


(def product {:root ".", :version "0.18.1", :key :clci, :release-prefix "clci"})


;; (derive-tag-name-for-latest-release product)

;; (get-commit-from-tag "clci-latest")

;; (get-all-tags-of-commit "105c0326bd3ced3f626f077eb8a5e4a82ceeb1ef")


(defn get-product-latest-release-name
  "Get the name of the last released version of a product.
   Takes the `product` as defined by the `:clci.repo/product` spec and
   uses the git cli to get the latest released version of the product.
   Returns a string with the product's latest release name.
   Internally uses the convention that a `<release-prefix>-latest`
   tag exisits for the latest release of a product to find the version of the
   latest release tagged with this special tag.
   
  !!!! Example
   
      ```clojure
      (def product {:root \".\", :version \"0.18.0\", 
                    :key :clci, :release-prefix \"clci\"})
      (get-product-latest-release-name product) ; -->  \"clci-0.18.1\"
      ```
   "
  [product]
  (->> product
       derive-tag-name-for-latest-release
       get-commit-from-tag
       get-all-tags-of-commit
       (filter (fn [tag]
                 (and
                   (some? (re-find (re-pattern semver-regex-pattern) tag))
                   (some? (re-find (re-pattern release-prefix-regex-pattern) tag)))))
       flatten
       first))



(comment
  (get-product-latest-release-name product)
)

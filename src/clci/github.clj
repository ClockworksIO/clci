(ns clci.github
  "This module provides several functions to interact with Github
	using the official Github REST API.
   
  **Info**: The module is intended as a raw and low level implementation to interact with Github.
    There is probably a high level implementation of the task you would like to achieve (i.e.
    get information about a specific release). Please have a look first if such a high level
    implementation exists! They are commonly a better place to start i.e. because they support
    more than one SCM provider platform.
  
  **note**: This module contains code based on https://github.com/borkdude/gh-release-artifact."
  (:require
    [babashka.fs :as fs]
    [babashka.http-client :as http]
    [cheshire.core :as cheshire]
    [clci.git :as git]
    [clci.util.core :refer [slurp-env-file]]
    [clojure.string :as str]))


(defn- gh-token
  "Get the Github access token from the environment.
   Reads the token in priority
   1. `.repl.env` file if present
   2. `.env` file if present
   3. From the environment variables otherwise"
  []
  (cond
    (fs/exists? ".repl.env")
    (:GITHUB_TOKEN (slurp-env-file ".repl.env"))
    (fs/exists? ".env")
    (:GITHUB_TOKEN (slurp-env-file ".env"))
    :else
    (System/getenv "GITHUB_TOKEN")))


(defn- path
  "Build a path from a vector of `parts` (strings)."
  [& parts]
  (str/join "/" parts))


(defn- ->query-string
  "Takes a map `params` of key-value pairs and builds an url
  query string from it."
  [params]
  (->> (map
         (fn [[k v]] (str (name k) "=" v))
         params)
       (str/join "&")))


(def endpoint
  "Github REST APIendpoint."
  "https://api.github.com")


(defn- release-endpoint
  "Get the release endpoint for a given `owner` and `repo`."
  [owner repo]
  (path endpoint "repos" owner repo "releases"))


(defn- with-headers
  "Add the gh authorization token to the request `req`."
  [req]
  (update req :headers assoc
          "Authorization" (str "token " (gh-token))
          "Accept" "application/vnd.github.v3+json"
          "X-GitHub-Api-Version" "2022-11-28"))


(defn- with-query-params
  "Add query params to a given base url."
  [base-url params]
  (str base-url "?" (->query-string params)))


(defn list-releases
  "Get a list of all releases.
  Takes the name of the `owner` and the name of the `repo`.
  Optionally takes either a third `limit` parameter which is an int from 1..100
  to limit the number of releases fetched from the server.
  When fetching the last 100 releases is not enough the function can be called with four
  arguments: `owner`, `repo`, `paer-page` and `page` where per-page is the number of
  results per page and page is the page number to fetch."
  ([owner repo] (list-releases owner repo 30))
  ([owner repo limit] (list-releases owner repo limit 1))
  ([owner repo per-page page]
   (-> (http/get (with-query-params (release-endpoint owner repo) {:per_page per-page :page page})
                 (with-headers {}))
       :body
       (cheshire/parse-string true))))


(defn get-release-by-tag-name
  "Get a specific release.
  Takes the name of the `owner`, the name of the `repo` and the name of the `tag` of the release."
  [owner repo tag]
  (-> (http/get (path (release-endpoint owner repo) "tags" tag)
                (with-headers {}))
      :body
      (cheshire/parse-string true)))


(defn get-latest-release
  "Get the latest release.
  Takes the name of the `owner` and the name of the `repo`."

  [owner repo]
  (-> (http/get (path (release-endpoint owner repo) "latest")
                (with-headers {}))
      :body
      (cheshire/parse-string true)))


(defn create-release
  "Create a new release.
  Takes the following argument map:
  | key                 | description                         |
  | --------------------|-------------------------------------|
  | `:owner`            | (required!) Owner of the repository.
  | `:repo`             | (required!) Name of the repository.
  | `:tag`              | (required!) Name of the tag for the release. Should follow SemVer!.
  | `:commit`           | (optional) SHA of the commit of the release. When not provided, the current commit is used.
  | `:draft`            | (optional) If true, the release is marked as draft. Defaults to true.
  | `:pre-release`      | (optional) If true, the release is marked as a pre-release.
  | `:target-commitish` | (optional) Specifies the commitish value that determines where the Git tag is created from.`"
  [{:keys [owner repo tag commit draft pre-release target-commitish]
    :or   {draft  true
           target-commitish (or commit (git/current-commit))}}]
  (-> (http/post (release-endpoint owner repo)
                 (with-headers
                   {:body (cheshire/generate-string (cond-> {:tag_name tag
                                                             :name tag
                                                             :draft draft}
                                                      target-commitish (assoc :target_commitish target-commitish)
                                                      pre-release      (assoc :prerelease pre-release)))}))
      :body
      (cheshire/parse-string true)))


(defn get-tag
  "Get a tag object.
  Takes the `owner` and the `repo` name and the `tag`."
  [owner repo tag]
  (let [url   (-> (http/get (path endpoint "repos" owner repo "git" "ref" "tags" tag)
                            (with-headers {}))
                  :body
                  (cheshire/parse-string true)
                  :object
                  :url)]
    (-> (http/get url (with-headers {}))
        :body
        (cheshire/parse-string true))))


(defn get-all-tag-refs
  "Get all tags.
   Uses the Github refs API to get all tag refs.
   Takes the `owner` and the `repo` name and the `tag`.
   Returns a list of git refs pointing to a tag."
  [owner repo]
  (-> (http/get (path endpoint "repos" owner repo "git" "refs" "tags") (with-headers {}))
      :body
      (cheshire/parse-string true)))


(defn tag-ref->tag
  "Transform a github tag ref into a tag.
   Takes a single tag ref as returned by the GH API and returns a single
   tag in map form.
   **Example result**:
   ```clojure
   {:name \"clci-0.15.0\"
    :sha \"6c7ec5872d417472f727f3d8e2a35fddad8ab593\"
    :ref \"refs/tags/clci-0.15.0\"
    :url \"https://api.github.com/repos/ClockworksIO/clci/git/refs/tags/clci-0.15.0\"
   }
   ```"
  [ref]
  {:name (-> (:ref ref) (str/split #"refs/tags/") last)
   :commit-sha (get-in ref [:object :sha])
   :ref (:ref ref)
   :url (:url ref)})


(defn tag-refs->tags
  "Transform a list of github tag refs to a vector of tags.
   Takes a list `tag-refs` with tag refs as returned from the GH API and transformes
   each ref to a tag representation."
  [tag-refs]
  (mapv tag-ref->tag tag-refs))



(defn get-tree
  "Returns a single tree using the SHA1 value or ref name for that tree.
   Takes the `owner` and the `repo` name and the `tree-sha` for a specific tree. 
   Optionally takes the `recursive?` option. If set, then the tree is fetched
   recursively."
  ([owner repo tree-sha] (get-tree owner repo tree-sha false))
  ([owner repo tree-sha recursive?]
   (let [uri (if recursive?
               (with-query-params (path endpoint "repos" owner repo "git" "trees" tree-sha) {:recursive true})
               (path endpoint "repos" owner repo "git" "trees" tree-sha))]
     (-> (http/get uri (with-headers {}))
         :body
         (cheshire/parse-string true)))))


(defn get-content
  "Returns a single tree using the SHA1 value or ref name for that tree.
   Takes the `owner` and the `repo` name, the `resource-path` denoting the
   path of the file or directory to fetch relativer to the repository root
   and the `ref` option to specify the ref (i.e. a branch name) that 
   should be uses to fetch the content."
  [owner repo resource-path ref]
  (-> (http/get (with-query-params (path endpoint "repos" owner repo "contents" resource-path) {:ref ref}) (with-headers {}))
      :body
      (cheshire/parse-string true)))

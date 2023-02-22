(ns clci.gh.core
  "This module provides several functions to interact with Github
	using the official Github REST API.
  This module contains code based on https://github.com/borkdude/gh-release-artifact"
  (:require
    [babashka.http-client :as http]
    [cheshire.core :as cheshire]
    [clci.git :as git]
    [clojure.string :as str]))


(defn- gh-token
  "Get the Github access token from the environment."
  []
  (System/getenv "GITHUB_TOKEN"))


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
  (-> (http/get (path endpoint "repos" owner repo "git" "ref" "tags" tag)
                (with-headers {}))
      :body
      (cheshire/parse-string true)))


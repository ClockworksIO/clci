(ns user)

(require '[clci.git :as git])
(require '[clci.gh.core :as gh])
(require '[clci.repo :as rp])

(require '[clci.conventional_commit :as cc])

(require '[clojure.string :as str])

(require '[babashka.http-client :as http])
(require '[cheshire.core :as cheshire])


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



(def repo         (rp/read-repo))
(def scm-owner    (get-in repo [:scm :provider :owner]))
(def scm-repo     (get-in repo [:scm :provider :repo]))


(def all-releases (list-releases scm-owner scm-repo))



(filter
  #(and (false? (:prerelease %)) (false? (:draft %)))
  '({:draft false :key 1} {:draft true :key 2} {:draft false :prerelease false :key 3}))

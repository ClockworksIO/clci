(ns clci.release
  (:require
    [babashka.fs :as fs]
    [clci.conventional-commit :as cc]
    [clci.git :as git]
    [clci.github :as gh]
    [clci.repo :as rp]
    [clci.semver :as sv]
    [clojure.spec.alpha :as spec]
    [clojure.string :as str]
    [miner.strgen :as sg]))



(def commit-hash-re #"[0-9a-f]{5,40}")


(spec/def ::hash
  (spec/spec
    (spec/and string? #(re-matches commit-hash-re %))
    :gen #(sg/string-generator commit-hash-re)))


(spec/def ::version #(re-matches sv/semver-re %))
(spec/def ::name string?) ; TODO: should follow the `<prefix> - <semver>` format
(spec/def ::tag string?) ; TODO: should follow the `<prefix> - <semver>` format
(spec/def ::commit (spec/keys :req-un [::hash]))

(spec/def ::release (spec/keys :req-un [::version ::commit ::name ::tag]))



(defn map-on-map-values
  "Apply the given function `f` on all values of the map `m`."
  [m f]
  (reduce (fn [altered-map [k v]] (assoc altered-map k (f v))) {} m))


;; Taken from https://blog.mrhaki.com/2020/04/clojure-goodness-checking-predicate-for.html
(defn- any
  "Test if the given predicate `pred` is true for at least one element of `coll`."
  [pred coll]
  ((comp boolean some) pred coll))


;; (defn no-prefix?
;;   "Tests if the given `project` does not use a release prefix."
;;   [project]
;;   (:no-release-prefix project))


(defn release-prefix
  "Get the release prefix of the specified `project`."
  [project]
  (str (:release-prefix project)  "-"))


(defn str-split-first
  "Split the given string `s` using the regex `re` on the first occurence of `re`.
   See: https://stackoverflow.com/a/31146456/5841420"
  [re s]
  (clojure.string/split s re 2))


(defn str-split-last
  "Split the given string `s` using the regex `re` on the last occurence of `re`.
   See: https://stackoverflow.com/a/31146456/5841420"
  [re s]
  (let [pattern (re-pattern (str re "(?!.*" re ")"))]
    (str-split-first pattern s)))


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
  "Predicate to test if the given `project` requires a new release.
   Takes the latest release of the project `latest-release` and compares
   version numbers (SemVer)."
  [project latest-release]
  (sv/newer? (:version project) (:version latest-release)))


(defn identify-release-prefix
  "Takes the name of a release and extracts the release prefix.
   The `name` of the release must be a string following the SemVer specification."
  [name]
  (let [parts (str-split-last #"-" name)]
    (cond
      (and (= 1 (count parts)) (sv/valid-semver-str? (first parts))) :no-valid-prefix
      (and (= 2 (count parts)) (sv/valid-semver-str? (get parts 1))) (first parts)
      :else :no-valid-prefix)))


(defn release-name->version
  "Get the version from a `release-name`. Expects the release-name to follow the
   <release-prefix>-<semver> schema!"
  [release-name]
  (let [parts (str/split release-name #"-")]
    (cond
      ;; no release prefix found
      (and (= (count parts) 1) (sv/valid-semver-str? (first parts))) (first parts)
      ;; found a release with prefix, does not use snapshot extension
      (and (> (count parts) 1) (sv/valid-semver-str? (last parts))) (last parts)
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
        (map-on-map-values $ (fn [releases] (last (sort-by :published_at releases))))))


(defn- get-latest-releases
  "Get the latest release for each project.
   Takes the `repo` config and returns a map where the keys are the prefix of the release (string) and
   The value is the latest release with this prefix. Releases with no or invalid prefix are grouped
   under the `:no-valid-prefix` key.
   **Warning**: Only supports Github Releases at this time!"
  [repo]
  (let [scm-owner       (get-in repo [:scm :provider :owner])
        scm-repo        (get-in repo [:scm :provider :repo])
        all-releases    (gh/list-releases scm-owner scm-repo)]
    (case (get-in repo [:scm :provider :name])
      ;; scm == github
      :github (group-gh-releases-by-prefix all-releases)
      ;; otherwise - not supported
      (ex-info "Only Github is supported as SCM provider!" {}))))


(defn get-latest-release
  "Get the latest release of the project.
   Takes the `repo` configuration and optionally a `project-key` to get the latest release
   for a specific project. The project-key is required if more than one project is present!"
  ([repo]
   (when-not (rp/single-project?)
     (ex-info "Getting the latest release requires a specific project key if the repo hast more than one project!" {}))
   (case (get-in repo [:scm :provider :name])
     ;; scm == github
     :github (let [repo-name (get-in repo [:scm :provider :repo])
                   owner     (get-in repo [:scm :provider :owner])
                   release 	(gh/get-latest-release owner repo-name)
                   tag       (gh/get-tag owner repo-name (:tag_name release))]
               {:commit  {:hash (:sha tag)}
                :tag     (or (:tag_name release) "0.0.0")
                :name    (or (:name release) "0.0.0")
                :version (or (release-name->version (:name release)) "0.0.0")})
     (ex-info "Only Github is supported as SCM provider!" {})))
  ([repo project-key]
   (case (get-in repo [:scm :provider :name])
     :github (ex-info "Not implemented yet!" {})
     (ex-info "Only Github is supported as SCM provider!" {}))))


(defn prepare-new-releases-impl
  "Calculate which projects need a new release and prepare all data required to create
   a new release."
  [repo latest-releases]
  (as-> latest-releases $
        (filter
          (fn [[prefix release]]
            (when-let  [project (rp/get-project-by-release-prefix prefix repo)]
              (new-release-required? project release)))
          $)
        (keys $)
        (map (fn [prefix] [prefix (rp/get-project-by-release-prefix prefix repo)]) $)
        (into (hash-map) $))
  ;; TODO: use new version info not the old release info!
  )


(defn create-releases
  "Create all releases.
   Identifies the projects that where changed (new version) since the last release found on
   the SCM/Release platform and creates new releases for each of them."
  []
  (let [repo 								(rp/read-repo)
        all-latest-releases (get-latest-releases repo)
        to-be-released 			(prepare-new-releases-impl repo all-latest-releases)]
    ;; scm == github
    (case (get-in repo [:scm :provider :name])
      :github (let [repo-name (get-in repo [:scm :provider :repo])
                    owner     (get-in repo [:scm :provider :owner])]
                (doseq [r to-be-released]
                  (println "would create release: " r)
                  (gh/create-release {:owner owner :repo repo :tag (str (release-prefix (:version r))) :draft false :pre-release false})
                  ;; TODO: add options to set as draft or pre-release
                  )
                (ex-info "Only Github is supported as SCM provider for Releases!" {})))))



(defn derive-current-commit-version-single-project-impl
  "Implementation of `clci.tools.release/derive-current-commit-version-single-project`.
   Takes a collection `amended-commit-log` with the amended commit history since the last
   release and the `project` from the repositorie's repo.edn configuration."
  [amended-commit-log project]
  (let [version-increments (->> amended-commit-log (map (comp derive-version-increment :ast)) (remove nil?))]
    {(:key project)
     (->> version-increments
          (reduce (fn [acc v-incr]
                    (inc-version acc v-incr))
                  (sv/version-str->vec (:version project)))
          (sv/version-vec->str))}))


(defn derive-current-commit-version-single-project
  "Derive the version of the current commit for a single project repository."
  []
  (let [repo                 (rp/read-repo)
        ;; get the last release using the gh api
        latest-release       (get-latest-release repo)
        ;; get a git log of all commits since the latest release and amend it with the commit
        ;; message ast
        amended-commit-log   (amend-commit-log (git/commits-on-branch-since {:since (get-in latest-release [:commit :hash])}))
        ;; get the project, the version information is required
        project              (first (rp/get-projects))]
    ;; using all information we collected we can now calculate the new version
    (derive-current-commit-version-single-project-impl amended-commit-log project)))


(defn affected-projects
  "Tests which of the projects are affected by the commit.
   Does so by checking if the changes happened in the root of each project.
   Takes an amended `commit` and the `projects` of the repo. "
  [commit projects]
  (->> projects
       (map (fn [proj]
              (when (any (fn [f] (fs/starts-with? f (:root proj))) (:files commit))
                [(:key proj) (derive-version-increment (:ast commit))])))
       (remove nil?)))


(defn derive-current-commit-all-versions-impl
  "Implementation to caculate the version of all projects based on the commit history.
   Takes a collection `amended-commit-log` with the amended commit history since the last
   release, the `projects` from the repositories repo.edn configuration."
  [amended-commit-log projects]
  (let [version-increments  (map (fn [commit] (affected-projects commit projects)) amended-commit-log)
        with-update         (fn [acc increments]
                              (reduce
                                (fn [a [key v-inc]] (assoc a key (inc-version (get a key) v-inc)))
                                acc increments))]
    (->
      (loop [head (first version-increments)
             tail (rest version-increments)
             acc  (into (sorted-map) (map (fn [p] [(:key p) (sv/version-str->vec (:version p))]) projects))]
        (if (empty? tail)
          acc
          (recur (first tail) (rest tail) (with-update acc head))))
      (map-on-map-values sv/version-vec->str))))



(defn derive-current-commit-all-versions
  "Caculate the version of all projects based on the commit history."
  []
  (let [repo 									(rp/read-repo)
        ;; get the last release using the gh api, that is the latest of any project releases!
        latest-release        (get-latest-release repo)
        ;; we need the version as an easy to manipulate datastructure, not just as string
        ;; last-release-version  (tf-release-version latest-release)
        commit-log            (amend-commit-log (git/commits-on-branch-since {:since (:commit latest-release)}))
        projects              (rp/get-projects)]
    (derive-current-commit-all-versions-impl commit-log projects)))


(defn derive-current-commit-version
  "Derive the version of the current codebase.
  Uses the latest release that exists and the git log which must follow the conventional commits
  specification. Depending on the type of the commit the new version will be calculated
  following the semantic versioning specs.
  Takes into account if the repo has a single project or many."
  []
  (if (rp/single-project?)
    (derive-current-commit-version-single-project)
    (derive-current-commit-all-versions)))

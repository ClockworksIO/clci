(ns user
  (:require
    [babashka.fs :as fs]
    [clojure.edn :as edn]
    [clojure.string :as str]))


(defn- join-paths
  "Takes an arboitrary number of (partital) paths and joins them together.
  Handles slashes at the end of the path.
	I.e. 
	```clojure
	(join-paths \"/some/base/path\" \"local/path/\") ; -> \"/some/base/path/local/path\"
  (join-paths \"/some/base/path/\" \"local/path\") ; -> \"/some/base/path/local/path\"
	```
	"
  [& parts]
  (as-> (map #(str/split % #"/") parts) $
        (apply concat $)
        (str/join "/" $)))


;; (join-paths "/home/foo" "some/go")

(def repo-config
  "Configuration of the repository."
  (atom nil))


(defn- read-edn-file
  "Read an edn file from the given `path`."
  [path]
  (when (fs/exists? path)
    (-> (slurp path)
        (edn/read-string))))


(defn- read-deps-edn
  "Read the deps.edn file from a project.
	Takes the the path of the `project-root` and an optional collection of `filter-keys`
	representing the keys that will be read from the deps.edn file."
  [project-root & {:keys [filter-keys] :or {filter-keys [:paths :deps]}}]
  (-> (read-edn-file (join-paths project-root "deps.edn"))
      (select-keys filter-keys)))


;; (read-deps-edn "./fintools")

(defn- read-bb-edn
  "Read the bb.edn file from a project.
	Takes the the path of the `project-root`."
  [project-root & {:keys [filter-keys] :or {filter-keys [:paths :deps]}}]
  (-> (read-edn-file (join-paths project-root "bb.edn"))
      (select-keys filter-keys)))


;; (read-bb-edn "./fintools")

(defn- read-repo-config
  "Read the repo config.
	Reads the repository configuration either from the default path
  or a given `path`."
  ([] (read-repo-config "repo.edn"))
  ([path]
   (as-> (slurp path) $
         (edn/read-string $)
         (assoc $ :projects (mapv
                              (fn [p]
                                (assoc p
                                       :deps-edn (read-deps-edn (:root p))
                                       :bb-edn (read-bb-edn (:root p))))
                              (:projects $)))
         (reset! repo-config $))))


(read-repo-config "repo.edn")

@repo-config



(def collect-all-paths
  ""
  [])

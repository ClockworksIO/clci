(ns clci.semver
  "Semantic Versioning related functionality.")


(def semver-re
  "Regular Expression to match version strings following the
  Semantic Versioning specification.
  See https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string."
  #"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")


(defn valid-version-tag?
  "Predicate to test if the given `tag` contains a string that follows
  the semantic versioning convention."
  [tag]
  (some? (re-matches semver-re tag)))

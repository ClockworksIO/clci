(ns clci.workflow-next.core
  ""
  (:require
    [clojure.spec.alpha :as spec :refer [fdef sdef]]))



(fdef job-spec)


(defn job
  [context get-artefact put-artefact feedback])

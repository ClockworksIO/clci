(ns utils
  "Utilities"
  (:require
    [clj-kondo.core :as clj-kondo]))


(defn kondo-lint
  "Run kondo to lint the code.
  Raises a non zero exit code if any errors are detected. Takes an optional
  argument `fail-on-warnings?`. If true the function also raises a non zero
  exit code when kondo detects any warnings."
  ([] (kondo-lint false))
  ([fail-on-warnings?]
   (let [{:keys [summary] :as results} (clj-kondo/run! {:lint ["src"]})]
     (clj-kondo/print! results)
     (when (or
             (if fail-on-warnings? (pos? (:warning summary)) false)
             (pos? (:error summary)))
       (throw (ex-info "Linter failed!" {:babashka/exit 1}))))))

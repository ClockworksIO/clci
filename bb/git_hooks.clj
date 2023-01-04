(ns git-hooks
  "This module defines several funtions that are invoked by Git hooks."
  (:require
    [babashka.process :refer [shell sh]]
    [clci.git-hooks-utils :refer [spit-hook changed-files]]
    [clci.pod :refer [valid-commit-msg?]]
    [clojure.term.colors :as c]
    [format :as fmt]))


;;
;; The code of this module is based on the following blog post: 
;; https://blaster.ai/blog/posts/manage-git-hooks-w-babashka.html
;;

(defmulti hooks (fn [& args] (first args)))


(defmethod hooks "install" [& _]
  (spit-hook "pre-commit")
  (spit-hook "commit-msg"))


;; Git 'pre-commit' hook.
(defmethod hooks "pre-commit" [& _]
  (println (c/blue "Executing pre-commit hook"))
  (let [files (changed-files)]
    (fmt/format "fix")
    (doseq [file files]
      (sh (format "git add %s" file)))))


;; (when-let [files (changed-files)]
;;   (apply sh "cljstyle" "fix" (filter clj? files))))

;; Git 'commit-msg' hook.
;; Takes the commit message and validates it conforms to the Conventional Commit specification
(defmethod hooks "commit-msg" [& _]
  (let [commit-msg (slurp ".git/COMMIT_EDITMSG")
        msg-valid? (true? (valid-commit-msg? commit-msg))]
    (println (valid-commit-msg? commit-msg) msg-valid?)
    (if msg-valid?
      (println (c/green "\u2713") " commit message follows the Conventional Commit specification")
      (do
        (println (c/red "\u2A2F") " commit message does NOT follow the Conventional Commit specification")
        (println (c/red "Abort commit!"))
        (System/exit -1)))))


;; Default handler to catch invalid hooks
(defmethod hooks :default [& args]
  (println (c/yellow "Unknown command: ") (c/red (first args))))

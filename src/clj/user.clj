(ns user)

(require '[clci.conventional-commit :refer [valid-commit-msg? msg->ast]])

(def commit-msg (slurp ".git/COMMIT_EDITMSG"))

(valid-commit-msg? commit-msg)

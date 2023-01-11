(ns user)

(use 'clci.conventional-commit :reload)

(def commit-msg (slurp ".git/COMMIT_EDITMSG"))

(require '[instaparse.core :as insta])

(-> (insta/parser grammar) (insta/parse commit-msg))


(def msg 
(str
      "feat: switch to new API #RR-33\n\n"
      "BREAKING CHANGE: With this commit we are switching to the new API. Please update your access token!\n\n"
      "# Please enter the commit message for your changes. Lines starting\n"
      "#  with '#' will be ignored, and an empty message aborts the commit.\n"
      "#\n"
      "# Date:      Thu Dec 8 17:01:23 2022 +0100\n"
      "# On branch feat/rr-2\n"
      "# Changes to be committed:\n"
      "#       renamed:    ci-bb/src/clj/ci_bb/pod.clj -> ci-bb/src/clj/ci_bb/main.clj")
  )



(-> (insta/parser grammar) (insta/parse msg {:trace true :total true}))

(-> (insta/parser grammar) (insta/parses msg))

(-> (insta/parser grammar) (insta/parse msg {:trace true}) (insta/get-failure))

;(= (-> (insta/parser grammar) (insta/parse msg)) result)

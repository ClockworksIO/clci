(ns user)


(comment 
  (use 'clci.conventional-commit :reload)

  (def commit-msg (slurp ".git/COMMIT_EDITMSG"))

  (require '[instaparse.core :as insta])

  (-> (insta/parser grammar) (insta/parse commit-msg))


  (def msg 
    (str
      "feat: implements pod optimizations of #7\n"
      "\n"
      "This commit updates how the pod is build. This includes\n"
      "- writing a correct Pom file according to babashkas pod spec\n"
      "- including different platforms and architectures for pods\n"
      "- versioned file names for pods\n"
      "- cleaning everything up\n"
      "# Please enter the commit message for your changes. Lines starting\n"
      "# with '#' will be ignored, and an empty message aborts the commit.\n"
      "#\n"
      "# On branch feat/clci-7\n"
      "# Changes to be committed:\n"
      "# new file:   .babashka/clci-0.1.2.main.metadata.cache\n"
      "# modified:   bb.edn\n"
      "# modified:   bb/build.clj\n"
      "# modified:   build.clj\n"
      "# modified:   deps.edn\n"
      "# deleted:    manifest.edn\n"
      "# new file:   resources/clockworksio/clci/pod-manifest.edn\n"
      "# modified:   src/clj/user.clj\n"
      "# modified:   test/clci/conventional_commit_test.clj\n"
      "#\n")
    )



  (-> (insta/parser grammar) (insta/parse msg {:trace true :total true}))

  (-> (insta/parser grammar) (insta/parses msg))

  (-> (insta/parser grammar) (insta/parse msg {:trace true}) (insta/get-failure))

  ;(= (-> (insta/parser grammar) (insta/parse msg)) result)

  )

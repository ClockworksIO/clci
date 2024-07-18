(ns clci.dialog-test
  "Tests for the build-in dialog mechanism"
  (:require
    [clci.assistant.dialog :refer [find-in-step]]
    [clojure.test :refer [deftest testing is]]))



(deftest dialog-history-tools
  (let [history   [{:step :welcome-msg}
                   {:step :wait-before-start, :failure? false}
                   {:step :product-type-question-label}
                   {:step :select-product-type, :selected-options '({:name "App", :key :app})}
                   {:step :select-template-label}
                   {:step :select-template, :selected-options '({:name "Clojure", :key :clojure})}
                   {:step :product-name-label}
                   {:step :product-name, :input "aProductAwesome"} {:step :product-nondefault-root-label}
                   {:step :product-nondefault-root-select, :selected-options '({:name "Yes", :key :yes})}
                   {:step :product-root-label}
                   {:step :product-root, :input "./a-path"}
                   {:step :use-clci-actions-label}
                   {:step :select-action-aliases, :selected-options ()}]]
    (testing "Test the various utility functions for dialog histories."
      ;; some full path specifiers
      (is (=
            (find-in-step history :product-nondefault-root-select :selected-options 0 :name)
            (find-in-step history :product-nondefault-root-select :selected-options first :name)
            "Yes"))
      (is (=
            (find-in-step history :product-nondefault-root-select :selected-options 0)
            (find-in-step history :product-nondefault-root-select :selected-options first)
            {:key :yes :name "Yes"}))
      (is (=
            (find-in-step history :product-nondefault-root-select :selected-options)
            (find-in-step history :product-nondefault-root-select :selected-options)
            '({:key :yes :name "Yes"})))
      (is (=
            (find-in-step history :product-nondefault-root-select)
            (find-in-step history :product-nondefault-root-select)))
      ;; paths that do not exist should yield nil
      (is (nil? (find-in-step history :does-not-exist)))
      (is (nil? (find-in-step history :product-nondefault-root-select :text)))
      (is (nil? (find-in-step history :product-nondefault-root-select :selected-options second)))
      (is (nil? (find-in-step history :product-nondefault-root-select :selected-options 3))))))

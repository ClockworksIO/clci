(ns clci.workflow-test
  "This module provides tests for the `git` module."
  (:require
    [clci.actions :as actions]
    [clci.util :as u]
    [clci.workflow.reporter :refer [default-reporter]]
    [clci.workflow.runner :refer [get-job-history job-output? get-job-output-value derive-inputs run-trigger-impl]]
    [clci.workflow.workflow :refer [valid-workflow? find-workflows-by-trigger remove-disabled-workflows]]
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer [deftest testing is]]))


(defn log-has-message-with-type
  "Utility function to test if the given runner `log` (vector of runner messages) has a message
   of type `t`."
  [log t]
  (u/find-first #(= t (get % :msg-type)) log))


(def example-workflow-no-jobs
  {:name          "No Jobs Workflow"
   :key           :example-workflow-no-jobs
   :description   "A WOrkflow that has not a single job."
   :trigger       [:manual]
   :disabled?     false
   :jobs          []})


(def example-workflow
  {:name          "Example Workflow"
   :key           :example-workflow
   :description   "Some blah blah blah."
   :trigger       [:git/commit-msg :manual]
   :disabled?     false
   :jobs
   [{:ref     :a-random-int
     :action  actions/create-random-integer-action
     :inputs  {:maximum 50}}
    {:ref     :increment-int
     :action  actions/increment-integer-action
     :inputs  {:number :!job.a-random-int/number}}]})


(def example-workflows
  "Get all workflows set for the "
  [example-workflow])


(def example-history
  "Example history of a workflow execution."
  [{:context  {:job {:ref     :a-random-int,
                     :inputs  {}}},
    :output   {:number 691},
    :failure  false}])


(deftest workflow-specs
  (testing "Test if workflows follow the spec. "
    (is (valid-workflow? example-workflow))
    (is (not (valid-workflow? example-workflow-no-jobs)))))


(deftest workflow-filter
  (testing "Test workflow filtering by disabled? and trigger. "
    (is (= 0
           (-> example-workflows
               (find-workflows-by-trigger :foo)
               (remove-disabled-workflows)
               (count))))
    (is (= 1
           (-> example-workflows
               (find-workflows-by-trigger :manual)
               (remove-disabled-workflows)
               (count))))
    (is (= 0
           (-> example-workflows
               (assoc-in [0 :disabled?] true)
               (find-workflows-by-trigger :manual)
               (remove-disabled-workflows)
               (count))))))


(deftest derive-job-inputs
  (testing "Run tests on the functions required to derive the input of a job."
    (is (= (get example-history 0) (get-job-history example-history :a-random-int)))
    (is (job-output? :!job.some-job/an-output))
    (is (job-output? :!job.a-random-int/number))
    (is (not (job-output? :job.some-job/an-output)))
    (is (not (job-output? 1)))
    (is (not (job-output? :some/emaple)))
    (is (= (get-in example-history [0 :outputs :number]) (get-job-output-value example-history :a-random-int :number)))
    (is (= {:number (get-in example-history [0 :outputs :number])} (derive-inputs (get-in example-workflow [:jobs 1]) example-history)))))


(deftest run-not-existing-trigger-workflow
  (testing "Run trigger for the example workflow that does not start it."
    (is (thrown? Exception (run-trigger-impl :foo example-workflows default-reporter)))))


(deftest run-workflows-no-jobs
  (testing "Run workflows that don't have jobs. "
    (is (thrown? Exception (run-trigger-impl :manual [example-workflow-no-jobs] default-reporter)))))


(deftest run-invalid-workflows
  (testing "Run workflows not following the workflow spec. "
    (is (thrown? Exception (run-trigger-impl :manual {} default-reporter)))))


(deftest run-disabled-workflows
  (testing "Run workflows that are disabled. "
    (is (thrown? Exception (run-trigger-impl :manual (assoc-in example-workflows [0 :disabled?] true) default-reporter)))))


(deftest run-single-example-workflow
  (testing "Run a single example workflow with a manual trigger."
    (let [run-result  (run-trigger-impl :manual example-workflows default-reporter {:debug? false})
          history     (->> run-result
                           :example-workflow
                           (u/find-first (fn [m] (= :clci.workflow.runner/finished (:msg-type m))))
                           :history)]
      (pprint run-result)
      (is (contains? run-result (:key example-workflow)))
      (is (log-has-message-with-type (get run-result :example-workflow) :clci.workflow.runner/workflow-start))
      (is (not (log-has-message-with-type (get run-result :example-workflow) :clci.workflow.runner/error)))
      (is (not (log-has-message-with-type (get run-result :example-workflow) :clci.workflow.runner/failure)))
      (is (= (get-in history [0 :outputs :number])
             (get-in history [1 :context :job :inputs :number])))
      (is (= (inc (get-in history [0 :outputs :number] 0))
             (get-in history [1 :outputs :number])))
      (is (= (inc (get-in history [1 :context :job :inputs :number]))
             (get-in history [1 :outputs :number]))))))

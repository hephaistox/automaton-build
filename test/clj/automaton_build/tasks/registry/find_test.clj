(ns automaton-build.tasks.registry.find-test
  (:require
   [automaton-build.tasks.registry.find :as sut]
   [clojure.test                        :refer [deftest is testing]]))

(def stub
  (into
   {}
   [['clean
     {:doc "Clean cache files for compiles, and logs"
      :name :clean
      :la-test {:cmd ["bb" "clean"]}
      :task-fn 'fn-1}]
    ['clean-hard
     {:doc
      "Clean all files which are not under version control (it doesn't remove untracked file or staged files if there are eligible to `git add .`)"
      :la-test {:cmd ["bb" "clean-hard"]
                :process-opts {:in "q"}}
      :name :clean-hard
      :pf :clj
      :task-fn 'fn-2}]]))

(deftest task-selection-test
  (testing "An existing symbol is found"
    (is (= :clean
           (-> (sut/task-selection stub ['clean])
               first
               :name))))
  (testing "Registry is seen as a map"
    (is (= :clean
           (-> (sut/task-selection stub ['clean])
               first
               :name))))
  (testing "Not found symbol" (is (empty? (sut/task-selection stub ['not-existing-task])))))

(deftest task-map-test
  (testing "Check normal task found" (is (= 'fn-1 (:task-fn (sut/task-map stub "clean")))))
  (testing "An hidden task is found" (is (= 'fn-2 (:task-fn (sut/task-map stub "clean-hard"))))))

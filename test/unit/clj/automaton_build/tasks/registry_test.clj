(ns automaton-build.tasks.registry-test
  (:require
   [automaton-build.tasks.registry :as sut]
   [clojure.test                   :refer [deftest is testing]]))

(def stub
  (into
   {}
   [['clean
     {:doc "Clean cache files for compiles, and logs"
      :name :clean
      :la-test {:cmd ["bb" "clean"]}
      :task-fn 'automaton-build.tasks.clean/clean}]
    ['clean-hard
     {:doc
      "Clean all files which are not under version control (it doesn't remove untracked file or staged files if there are eligible to `git add .`)"
      :la-test {:cmd ["bb" "clean-hard"]
                :process-opts {:in "q"}}
      :name :clean-hard
      :mandatory-config? true
      :pf :clj
      :task-fn 'automaton-build.tasks.clean-hard/clean-hard}]]))

(deftest task-names-test
  (testing "Tasks are found"
    (is (= #{'clean-hard 'clean}
           (-> (sut/task-names stub)
               set)))))

(deftest not-mandatory-test
  (testing "Find not mandatory tasks" (is (= 1 (count (sut/not-mandatory stub))))))

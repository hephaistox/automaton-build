(ns automaton-build.cicd.hosting-test
  (:require
   [automaton-build.cicd.hosting :as sut]
   [clojure.test                 :refer [deftest is testing]]))

(deftest hosting-valid?
  (testing "Check if non existing command is caught"
    (is (not (sut/hosting-installed?* "non-existing-cc-cmd")))))

(comment
  (sut/prod-ssh ".")
  (sut/upsert-cc-app "foo" ".")
  ;
)

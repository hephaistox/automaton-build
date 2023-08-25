(ns automaton-build.adapters.hosting-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.hosting :as sut]))

(deftest hosting-valid?
  (testing "Check if non existing command is caught"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Clever cloud is not installed properly"
                          (sut/hosting-installed? "non-existing-cc-cmd")))))

(comment
  (sut/prod-ssh ".")

  (sut/upsert-cc-app "foo" ".")
  ;
  )

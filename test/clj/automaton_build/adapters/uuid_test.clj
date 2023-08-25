(ns automaton-build.adapters.uuid-test
  (:require
   [clojure.test :refer [is testing deftest]]
   [automaton-build.adapters.uuid :as sut]))

(deftest time-based-uuid
  (testing "Is a uuid"
    (is (uuid? (sut/time-based-uuid))))
  (testing "Timed based uuid are in the right order"
    (dotimes [_ 10]
      (let [uuid1 (sut/time-based-uuid)
            uuid2 (sut/time-based-uuid)]
        (is (compare (str uuid1)
                     (str uuid2)))))))

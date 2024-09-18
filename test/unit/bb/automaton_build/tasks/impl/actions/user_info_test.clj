(ns automaton-build.tasks.impl.actions.user-info-test
  (:require
   [automaton-build.tasks.impl.actions.user-info :as sut]
   [clojure.test                                 :refer [deftest is]]))

(deftest user-infos-test
  (is (= [:id :group-id] (keys (sut/user-infos ["f" "g"] false)))
      "Test if user infos is a map (so it is a success) with the two keys."))

(ns automaton-build.data.schema-test
  (:require
   [automaton-build.data.schema :as sut]
   [clojure.test                :refer [deftest is]]))

(deftest valid?-test
  (is (= "[\"should be a string\"]\n" (sut/humanize :string 12)))
  (is (nil? (sut/humanize :string "12"))))

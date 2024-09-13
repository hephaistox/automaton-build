(ns automaton-build.os.java-properties-test
  (:require
   [automaton-build.os.java-properties :as sut]
   [clojure.test                       :refer [deftest is testing]]))

(deftest get-java-properties-test
  (testing "Java properties are valid"
    (is (map? (sut/get-java-properties)))
    (is (< 2
           (-> (sut/get-java-properties)
               count)))))

(deftest split-property-value-test
  (testing "Property value is splitted correctly"
    (is (= ["a" "b"] (sut/split-property-value "a,b")))
    (is (= ["a"] (sut/split-property-value "a")))))

(comment
  (sut/get-java-property "heph-conf")
  ;
)

(ns automaton-build.adapters.deep-merge-test
  (:require
   [clojure.test :refer [is deftest testing]]

   [automaton-build.adapters.deep-merge :as sut]))

(deftest deep-merge-test
  (testing "last map has higher priority"
    (is (= {:one 1 :two {}}
           (sut/deep-merge {:one 1 :two 2}
                           {:one 1 :two {}}))))
  (testing "Two level of nest"
    (is (= {:one 1 :two {:three {:test true} :four {:five 5}}}
           (sut/deep-merge
            {:one 1 :two {:three 3 :four {:five 5}}}
            {:two {:three {:test true}}}))))

  (testing "Two level of nest"
    (is (= {:one {:two {:three "three" :nine 9}
                  :seven 7}
            :four {:five 5 :eight 8}
            :ten  10}
           (sut/deep-merge
            {:one {:two {:three 3}}
             :four {:five {:six 6}}}
            {:one {:seven 7 :two {:three "three" :nine 9}}
             :four {:eight 8 :five 5}
             :ten 10}))))
  (testing "Non conflicting keys are merged"
    (is (= {:one {:two 2 :three 3 :four 4 :five 5}}
           (sut/deep-merge
            {:one {:two 2 :three 3}}
            {:one {:four 4 :five 5}}))))
  (testing "Nil is working as an empty map"
    (is (= {:one 1 :two {:three 3}}
           (sut/deep-merge
            {:one 1 :two {:three 3}}
            nil))))
  (testing "Nil is working as an empty map in complex list"
    (is (= {:one 1 :two {:three 3
                         :fourth 4
                         :fifth 5}}
           (sut/deep-merge
            {:one 1 :two {:three 3}}
            nil
            {:one 1 :two {:fourth 4}}
            nil
            nil
            {:one 1 :two {:fifth 5}}))))
  (testing "Multiple maps are manager, last one is higher priority"
    (is (= {:one 4 :two {:three 6}}
           (sut/deep-merge
            {:one 1 :two {:three 3}}
            {:one 2 :two {:three 4}}
            {:one 3 :two {:three 5}}
            {:one 4 :two {:three 6}})))))

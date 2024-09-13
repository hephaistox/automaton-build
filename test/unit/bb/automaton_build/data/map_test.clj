(ns automaton-build.data.map-test
  (:require
   [automaton-build.data.map :as sut]
   [clojure.test             :refer [deftest is testing]]))

(deftest replace-keys-test
  (testing "Key is replaced correctly"
    (is (= {:a 3}
           (sut/replace-keys {:a 3
                              :b 2}
                             {:a 1})))
    (is (= {:a 3
            :b 2}
           (sut/replace-keys {:a 3}
                             {:a 1
                              :b 2})))
    (is (= {:a 3
            :b 2
            :c {:d 3}}
           (sut/replace-keys {:a 3}
                             {:a 1
                              :b 2
                              :c {:d 3}}))))
  (testing "Keys are not added when they don't exist in m2"
    (is (= {} (sut/replace-keys {:a 1} {})))
    (is (= {}
           (sut/replace-keys {:a 1
                              :b 2
                              :c 3}
                             {})))
    (is (= {:d 1}
           (sut/replace-keys {:a 1
                              :b 2
                              :c 3}
                             {:d 1})))
    (is (= {:a 1}
           (sut/replace-keys {:a 1
                              :b 2
                              :c 3}
                             {:a 30})))))

(deftest deep-merge-test
  (testing "last map has higher priority"
    (is (= {:one 1
            :two {}}
           (sut/deep-merge {:one 1
                            :two 2}
                           {:one 1
                            :two {}}))))
  (testing "Two level of nest"
    (is (= {:one 1
            :two {:three {:test true}
                  :four {:five 5}}}
           (sut/deep-merge {:one 1
                            :two {:three 3
                                  :four {:five 5}}}
                           {:two {:three {:test true}}}))))
  (testing "Two level of nest"
    (is (= {:one {:two {:three "three"
                        :nine 9}
                  :seven 7}
            :four {:five 5
                   :eight 8}
            :ten 10}
           (sut/deep-merge {:one {:two {:three 3}}
                            :four {:five {:six 6}}}
                           {:one {:seven 7
                                  :two {:three "three"
                                        :nine 9}}
                            :four {:eight 8
                                   :five 5}
                            :ten 10}))))
  (testing "Non conflicting keys are merged"
    (is (= {:one {:two 2
                  :three 3
                  :four 4
                  :five 5}}
           (sut/deep-merge {:one {:two 2
                                  :three 3}}
                           {:one {:four 4
                                  :five 5}}))))
  (testing "Nil is working as an empty map"
    (is (= {:one 1
            :two {:three 3}}
           (sut/deep-merge {:one 1
                            :two {:three 3}}
                           nil))))
  (testing "Nil is working as an empty map in complex list"
    (is (= {:one 1
            :two {:three 3
                  :fourth 4
                  :fifth 5}}
           (sut/deep-merge {:one 1
                            :two {:three 3}}
                           nil
                           {:one 1
                            :two {:fourth 4}}
                           nil
                           nil
                           {:one 1
                            :two {:fifth 5}}))))
  (testing "Multiple maps are manager, last one is higher priority"
    (is (= {:one 4
            :two {:three 6}}
           (sut/deep-merge {:one 1
                            :two {:three 3}}
                           {:one 2
                            :two {:three 4}}
                           {:one 3
                            :two {:three 5}}
                           {:one 4
                            :two {:three 6}})))))
(deftest sorted-map-nested-test
  (testing "Are maps sorted?"
    (is (= {:a 1
            :b 5
            :c 2
            :e {:a 2
                :b 5}}
           (sut/sorted-map-nested {:a 1
                                   :b 5
                                   :c 2
                                   :e {:a 2
                                       :b 5}})))
    (is (= {:a 1
            :b 5
            :c 2}
           (sut/sorted-map-nested {:b 5
                                   :a 1
                                   :c 2})))
    (is (= {:a 1
            :b 5
            :c 2
            :e {:a 2
                :b 5}}
           (sut/sorted-map-nested {:c 2
                                   :b 5
                                   :a 1
                                   :e {:b 5
                                       :a 2}})))))

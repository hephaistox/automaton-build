(ns automaton-build.utils.map-test
  (:require
   [automaton-build.utils.map :as sut]
   [clojure.test              :refer [deftest is testing]]))

(deftest sort-submap-test
  (testing "Are keywords first, symbol then and each sorted alphabetically"
    (is (= (hash {:sk {:a 4
                       :z 2
                       'aa 3
                       'foo 1}})
           (hash (sut/sort-submap {:sk {'foo 1
                                        :z 2
                                        'aa 3
                                        :a 4}}
                                  [:sk])))))
  (testing "Edge cases"
    (is (= {:sk {}} (sut/sort-submap {} [:sk])))
    (is (= {:sk {:foo :bar
                 :a :b
                 :sl {}}
            :sl {'bar :foo
                 'aa :bb}}
           (sut/sort-submap {:sk {:foo :bar
                                  :a :b}
                             :sl {'bar :foo
                                  'aa :bb}}
                            [:sk :sl])))
    (is (= {} (sut/sort-submap {})))))

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

(deftest update-k-v-test
  (testing "Update works on different tree length"
    (is (= {:a 2} (sut/update-k-v {:a 3} :a 2)))
    (is (= {:a 2
            :b {:d 3}}
           (sut/update-k-v {:a 2
                            :b {:d 10}}
                           :d
                           3)))
    (is (= {:a 3
            :b {:d 10}
            :c {:e {:f 5}
                :x 2}}
           (sut/update-k-v {:a 3
                            :b {:d 10}
                            :c {:e {:f 30}
                                :x 2}}
                           :f
                           5))))
  (testing "Non existing key is not added"
    (is (= {} (sut/update-k-v {} :b 3)))
    (is (= {:a 1} (sut/update-k-v {:a 1} :b 3)))
    (is (= {:a 1
            :c {:e 2}}
           (sut/update-k-v {:a 1
                            :c {:e 2}}
                           :b
                           3))))
  (testing "Values are not mistaken with key"
    (is (= {:c :a
            :b :d}
           (sut/update-k-v {:c :a
                            :b :d}
                           :a
                           5))))
  (testing "Multiple key references are updated"
    (is (= {:a 2
            :b {:a 2}}
           (sut/update-k-v {:a 1
                            :b {:a 5}}
                           :a
                           2)))
    (is (= {:c 5
            :b {:a 2
                :e {:a 2}}
            :g {:f {:a 2}}}
           (sut/update-k-v {:c 5
                            :b {:a 6
                                :e {:a 30}}
                            :g {:f {:a 20}}}
                           :a
                           2))))
  (testing "Different values replaced"
    (is (= {:a 2} (sut/update-k-v {:a {:test 15}} :a 2)))
    (is (= {:a 2} (sut/update-k-v {:a ["paths" "to" "a"]} :a 2)))))

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

(ns automaton-build.graph-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.graph :as sut]))

(def graph-data
  {'a {:edges {'b {}
               'c {}}
       :foo4 :bar4}
   'b {:edges {'d {}}
       :foo3 :bar3}
   'c {:edges {'external1 {}}
       :foo2 :bar2}
   'd {:foo :bar}})

(deftest remove-graph-layer-test
  (testing "Build one graph layer"
    (is (= [{'c {:edges {'external1 {}}, :foo2 :bar2}
             'd {:foo :bar}}

            {'a {:edges {'b {}}
                 :foo4 :bar4}
             'b {:edges {}
                 :foo3 :bar3}}]
           (sut/remove-graph-layer graph-data)))))

(def result
  (atom []))

(deftest topologically-ordered-doseq
  (testing "Check doseq is ordered in the right order"
    (is (do
          (reset! result [])
          (sut/topologically-ordered-doseq graph-data
                                           (fn [edge]
                                             (swap! result
                                                    #(conj % (first edge)))))
          (= @result
             ['c 'd 'b 'a]))))
  (testing "Work with graph with useless dependencies"
    (is (do
          (reset! result [])
          (sut/topologically-ordered-doseq (assoc graph-data
                                                  'e {:foo5 :bar5})
                                           (fn [edge]
                                             (swap! result
                                                    #(conj % (first edge)))))
          (= @result
             ['c 'd 'e 'b 'a])))))

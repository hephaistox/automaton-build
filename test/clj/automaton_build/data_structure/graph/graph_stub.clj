(ns automaton-build.data-structure.graph.graph-stub
  (:require
   [clojure.test :refer [deftest is testing]]))

(def graph-data
  {'a {:link-to {'b {}
                 'c {}}
       :foo4 :bar4}
   'b {:link-to {'d {}}
       :foo3 :bar3}
   'c {:link-to {'external1 {}}
       :foo2 :bar2}
   'd {:foo :bar}})

(defn edges-fn
  [graph]
  (->> graph
       (mapcat (fn [[node node-data]]
                 (mapv #(vector node %)
                       (-> node-data
                           :link-to
                           keys))))
       vec))

(defn nodes-fn
  [graph]
  (->> graph
       keys
       vec))

(defn src-in-edge [edge] (first edge))

(defn dst-in-edge [edge] (second edge))

(defn remove-nodes [graph nodes-to-remove] (apply dissoc graph (set nodes-to-remove)))

(deftest remove-nodes-test
  (testing "Remove some nodes is ok"
    (is (= {'a {:link-to {'b {}
                          'c {}}
                :foo4 :bar4}
            'b {:link-to {'d {}}
                :foo3 :bar3}}
           (remove-nodes graph-data ['c 'd])))))

(deftest nodes-test (testing "Are nodes found" (is (= ['a 'b 'c 'd] (nodes-fn graph-data)))))

(deftest edges-test
  (testing "Are edges found"
    (is (= [['a 'b] ['a 'c] ['b 'd] ['c 'external1]] (edges-fn graph-data)))))

(deftest source-from-edge-test
  (testing "Find a source in a edge" (is (= 'a (src-in-edge (first (edges-fn graph-data)))))))

(deftest dest-from-edge-test
  (testing "Find a dest in a edge" (is (= 'b (dst-in-edge (first (edges-fn graph-data)))))))

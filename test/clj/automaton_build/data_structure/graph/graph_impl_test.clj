(ns automaton-build.data-structure.graph.graph-impl-test
  (:require
   [automaton-build.data-structure.graph.graph-impl :as sut]
   [automaton-build.data-structure.graph.graph-stub :as graph-stub]
   [clojure.test                                    :refer
                                                    [deftest is testing]]))

(deftest remove-graph-layer-test
  (testing
    "Removing one layer should remove 'd, 'external-deps is not seen as it is not defined as a node"
    (is (= [#{'a}
            {'d {:foo :bar}
             'b {:link-to {'d {}}
                 :foo3 :bar3}
             'c {:link-to {'external1 {}}
                 :foo2 :bar2}}]
           (sut/remove-graph-layer graph-stub/nodes-fn
                                   graph-stub/edges-fn
                                   graph-stub/dst-in-edge
                                   graph-stub/remove-nodes
                                   graph-stub/graph-data))))
  (testing "Double application is ok"
    (is (= [#{'c 'b} {'d {:foo :bar}}]
           (->> graph-stub/graph-data
                (sut/remove-graph-layer graph-stub/nodes-fn
                                        graph-stub/edges-fn
                                        graph-stub/dst-in-edge
                                        graph-stub/remove-nodes)
                second
                (sut/remove-graph-layer graph-stub/nodes-fn
                                        graph-stub/edges-fn
                                        graph-stub/dst-in-edge
                                        graph-stub/remove-nodes)))))
  (testing "Triple application is ok"
    (is (= [#{'d} {}]
           (->> graph-stub/graph-data
                (sut/remove-graph-layer graph-stub/nodes-fn
                                        graph-stub/edges-fn
                                        graph-stub/dst-in-edge
                                        graph-stub/remove-nodes)
                second
                (sut/remove-graph-layer graph-stub/nodes-fn
                                        graph-stub/edges-fn
                                        graph-stub/dst-in-edge
                                        graph-stub/remove-nodes)
                second
                (sut/remove-graph-layer graph-stub/nodes-fn
                                        graph-stub/edges-fn
                                        graph-stub/dst-in-edge
                                        graph-stub/remove-nodes))))))

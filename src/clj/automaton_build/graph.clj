(ns automaton-build.graph
  "Naive implementation of graph

  As a user you'll need to define the following functions to define how to manipulate your graph:

  ```
(defn edges-fn
  [graph])

(defn nodes-fn
  [graph])

(defn src-in-edge
  [edge])

(defn dst-in-edge
  [edge])

(defn remove-nodes
  [graph nodes-to-remove])
  ```"
  (:require
   [automaton-build.data-structure.graph.graph-impl :as graph-impl]
   [automaton-build.log                             :as build-log]))

(defn topologically-ordered
  "Apply with side effects the `update-fn` on the `graph` while respecting the topological order
  Returns the nodes (i.e. in the sens of `nodes-fn`), ordered in such a way that if A requires B so, B is earlier in the sequence

  Params:
  * `nodes-fn` function taking one graph as a parameter, returns the list of nodes
  * `edges-fn` function taking one graph as a parameter,  return the list of edges
  * `dest-in-edge` function taking an edge as a parameter (as returned by edges-fn), return the destination of the oriented edge
  * `remove-nodes` function taking a graph as a first parameter and a collection of nodes to remove as a second parameter, returns the updated collection
  * `graph` the graph to explore"
  [nodes-fn edges-fn dest-in-edge remove-nodes graph]
  (loop [graph-to-process graph
         topologically-sorted-nodes []
         nb-nodes (->> graph-to-process
                       nodes-fn
                       count)]
    (let [[nodes-with-no-successor updated-graph] (graph-impl/remove-graph-layer nodes-fn
                                                                                 edges-fn
                                                                                 dest-in-edge
                                                                                 remove-nodes
                                                                                 graph-to-process)
          topologically-sorted-graph
          (apply conj topologically-sorted-nodes nodes-with-no-successor)]
      (cond
        (or (empty? nodes-with-no-successor) (neg? nb-nodes))
        (do (build-log/error "Cycle found in the graph")
            (build-log/debug-data {:graph graph-to-process
                                   :edges-with-no-successor nodes-with-no-successor}))
        (empty? updated-graph) topologically-sorted-graph
        :else (recur updated-graph topologically-sorted-graph (dec nb-nodes))))))

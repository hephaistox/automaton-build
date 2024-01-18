(ns automaton-build.data-structure.graph.graph-impl
  "Private namespace for graph implementation"
  (:require
   [clojure.set :as set]))

(defn remove-graph-layer
  "Remove all nodes in the `graph` with no predecessor (i.e. in the sense of function `dst-in-edge`)

  Returns a 2-tuple
  * The first value is the list of nodes without predecessors
  * The second is the graph updated with that predecessors removed both in term of edges and nodes

  This function could be applied again on the updated graph (the second parameter)
  If there are no cycle in the graph, it will end up to an empty graph.

  This is useful to build topoligical order
  Params:
  * see definition of `monorepo-app.data-structure.graph/topologically-order`
    "
  [nodes-fn edges-fn dst-in-edge remove-nodes graph]
  (let [nodes (->> (nodes-fn graph)
                   set)
        edges (edges-fn graph)
        dst-nodes (->> edges
                       (mapv dst-in-edge)
                       set)
        nodes-with-no-predecessor (apply sorted-set-by
                                         #(compare (name %1) (name %2))
                                         (-> nodes
                                             (set/difference dst-nodes)))]
    [nodes-with-no-predecessor (remove-nodes graph nodes-with-no-predecessor)]))

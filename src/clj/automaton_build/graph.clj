(ns automaton-build.graph
  "Basic graph management for building app"
  (:require
   [clojure.set :as set]))

(defn remove-graph-layer
  "Remove all edges in the `graph` with no successor
  * The first value is the list of edges without successors
  * The second is the graph updated with that successors removes both in term of edges and nodes

  This function could be applied again on the updated graph.
  If there are no cycle in the graph, it will end up to an empty graph.

  This is useful to build topoligical order"
  [graph]
  (let [edges (into #{} (keys graph))
        edges-with-no-successor (into {} (filter (fn [edge]
                                                   (empty? (set/intersection (into #{} (keys (:edges (second edge))))
                                                                             edges)))
                                                 graph))
        nodes-with-no-successor (keys edges-with-no-successor)
        graph-with-no-successor-removed (apply dissoc graph
                                               nodes-with-no-successor)
        graph-with-edges-updated (map (fn [[edge {:keys [edges]
                                                  :as data}]]
                                        [edge (assoc data
                                                     :edges (apply dissoc edges
                                                                   nodes-with-no-successor))])
                                      graph-with-no-successor-removed)]
    [edges-with-no-successor (into {}
                                   graph-with-edges-updated)]))

(defn topologically-ordered-doseq
  "Apply with side effects the `update-fn` on the `graph` while respecting the topological order
  Params:
  * `graph` the graph to explore
  * `body-fn` a function applied to an edge (so a pair of node and a map with `:app-name` and `:edges` the map of dependencies )"
  [graph body-fn]
  (loop [graph graph
         nb-nodes (count graph)]
    (let [[edges-with-no-successor updated-graph] (remove-graph-layer graph)]
      (doseq [edge-with-no-successor edges-with-no-successor]
        (body-fn edge-with-no-successor))

      (when (zero? nb-nodes)
        (throw (ex-info "Cycle found"
                        {:graph graph
                         :edges-with-no-successor edges-with-no-successor})))

      (when-not (empty? updated-graph)
        (recur updated-graph (dec nb-nodes))))))

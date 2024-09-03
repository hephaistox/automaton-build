(ns automaton-build.app.deps-graph.impl
  (:require
   [automaton-build.app.deps-edn    :as build-deps-edn]
   [automaton-build.graph           :as graph]
   [automaton-build.utils.namespace :as build-namespace]))

(defn add-hephaistox-deps
  "Creates a graph dependency of our apps, i.e. a map associating the lib symbol to a map containing:

  Params:
  * `apps` applications"
  [apps]
  (->> apps
       (map (fn [{:keys [deps-edn publication app-name]
                  :as app}]
              (let [as-lib (get publication
                                :as-lib
                                (build-namespace/namespaced-keyword "non-lib" app-name))]
                [as-lib (assoc app :hephaistox-deps (build-deps-edn/hephaistox-deps deps-edn))])))
       (into {})))

(defn nodes-fn
  [graph]
  (->> graph
       keys
       vec))

(defn edges-fn
  [graph]
  (->> graph
       (mapcat (fn [[app-name {:keys [hephaistox-deps]}]]
                 (mapv (fn [dep-lib] [app-name dep-lib]) hephaistox-deps)))
       vec))

(defn src-in-edge [edge] (first edge))

(defn dst-in-edge [edge] (second edge))

(defn remove-nodes [graph nodes-to-remove] (apply dissoc graph (set nodes-to-remove)))

(defn map-app-lib-to-app
  [deps-graph ordered-libs]
  (->> ordered-libs
       (mapv (fn [app-lib] (get deps-graph app-lib)))))

(defn topologically-sort
  "Sort topologically the graph
  Params:
  * `deps-graph`"
  [deps-graph]
  (->> deps-graph
       (graph/topologically-ordered nodes-fn edges-fn dst-in-edge remove-nodes)
       (map-app-lib-to-app deps-graph)))

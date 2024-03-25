(ns automaton-build.code-helpers.antq
  "Proxy to antq library to help with update of dependencies"
  (:require
   [antq.api]))

(defn antq-type
  [dep-map]
  (case (:type dep-map)
    :deps-edn (assoc dep-map :type :clojure)
    :bb-edn (assoc dep-map :type :clojure)
    :pom-xml (assoc dep-map :type :pom)
    dep-map))

(defn find-outdated-deps [deps-str] (antq.api/outdated-deps deps-str))

(defn update-deps!
  [deps-to-update]
  (->> deps-to-update
       (map #(antq-type %))
       antq.api/upgrade-deps!))

(ns automaton-build.data.map
  "Useful map utilities"
  (:require
   [clojure.walk :as walk]))

(defn replace-keys
  "Replace keys in `m2` with keys from `m1`. Similiar to merge but non-existen keys in first map won't be added. e.g. (replace-keys {:a 3 :b 2} {:a 1}) -> {:a 3}"
  [m1 m2]
  (->> (select-keys m1 (keys m2))
       (merge m2)))

(defn deep-merge
  "Deep merge nested maps.
  Last map has higher priority

  This code comes from this [gist](https://gist.github.com/danielpcox/c70a8aa2c36766200a95)"
  [& maps]
  (apply merge-with
         (fn [& args]
           (if (every? #(or (map? %) (nil? %)) args) (apply deep-merge args) (last args)))
         maps))

(defn sorted-map-nested
  "Turn map into sorted-map and apply it to all nested submaps."
  [m]
  (walk/prewalk (fn [item] (if (map? item) (into (sorted-map) item) item)) m))

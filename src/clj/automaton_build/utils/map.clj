(ns automaton-build.utils.map
  "Gather utility functions for maps"
  (:require
   [automaton-build.utils.comparators :as build-utils-comparators]
   [clojure.walk                      :as walk]))

(defn update-k-v
  "Update key value in map, works with keywords that are nested.
   e.g.  (update-k-v {:a {:b 1}} :b 3) -> {:a {:b 3}}
   Params:
   * `update-k` - keyword by which value will be replaced
   * `update-v` - value that it should be replaced with
   * `m` - map where key should be find`"
  [m update-k update-val]
  (walk/walk (fn [[k v]]
               (cond
                 (= k update-k) [k update-val]
                 (map? v) [k (update-k-v v update-k update-val)]
                 :else [k v]))
             identity
             m))

(defn sort-submap
  "Sort the elements of a submap in the map
  Params
  * `m` map
  * `ks` is a sequence of sequence of keys where the submap should be sorted"
  [m & kss]
  (if (empty? kss)
    m
    (let [[ks & rkss] kss]
      (-> (update-in m
                     ks
                     (partial into
                              (sorted-map-by
                               build-utils-comparators/comparator-kw-symbol)))
          (recur rkss)))))

(defn sorted-map-nested
  "Turn map into sorted-map and apply it to all nested submaps."
  [m]
  (walk/prewalk (fn [item] (if (map? item) (into (sorted-map) item) item)) m))

(defn deep-merge
  "Deep merge nested maps.
  Last map has higher priority

  This code comes from this [gist](https://gist.github.com/danielpcox/c70a8aa2c36766200a95)"
  [& maps]
  (apply merge-with
         (fn [& args]
           (if (every? #(or (map? %) (nil? %)) args)
             (apply deep-merge args)
             (last args)))
         maps))

(defn select-keys*
  "Like select-keys, but works on nested keys."
  [m v]
  (reduce
   (fn [aggregate next]
     (let [key-value (if (vector? next)
                       [(last next) (get-in m next)]
                       [next (get m next)])]
       (if (second key-value) (apply assoc aggregate key-value) aggregate)))
   {}
   v))

(defn replace-keys
  "Replace keys in `m2` with keys from `m1`. Similiar to merge but non-existen keys in first map won't be added. e.g. (replace-keys {:a 3 :b 2} {:a 1}) -> {:a 3}"
  [m1 m2]
  (->> (select-keys m1 (keys m2))
       (merge m2)))

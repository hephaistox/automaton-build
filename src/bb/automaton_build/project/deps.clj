(ns automaton-build.project.deps)

(defn get-src
  "Returns source directories."
  [deps-edn]
  (->> deps-edn
       :aliases
       vals
       (mapcat :extra-paths)
       (concat (:paths deps-edn))
       (filterv #(re-find #"src|test" %))))

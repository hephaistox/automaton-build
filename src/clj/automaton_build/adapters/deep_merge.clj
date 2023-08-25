(ns automaton-build.adapters.deep-merge
  "Merge maps and their sub-maps
See https://gist.github.com/danielpcox/c70a8aa2c36766200a95 ")

(defn deep-merge
  "Merge maps and their sub-maps"
  [& maps]
  (apply merge-with (fn [& args]
                      (if (every? #(or (map? %) (nil? %)) args)
                        (apply deep-merge args)
                        (last args)))
         maps))

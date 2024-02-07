(ns automaton-build.utils.seq (:refer-clojure :exclude [contains?]))

(defn contains?
  "Determine whether a sequence contains a given item"
  ([collection value]
   (when-let [sequence (seq collection)] (some #(= value %) sequence)))
  ([collection value & next]
   (when (contains? collection value) (apply contains? collection next))))

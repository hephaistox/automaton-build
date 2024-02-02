(ns automaton-build.utils.keyword
  "Utility functions for keywords."
  (:require
   [clojure.string :as str]))

(defn trim-colon
  "If string `s` starts with `:` char it is removed."
  [s]
  (if (= ":" (first s)) (rest s) s))

(defn keywordize
  "Change string to appropriate clojure keyword"
  [s]
  (-> (name s)
      str/lower-case
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword)))

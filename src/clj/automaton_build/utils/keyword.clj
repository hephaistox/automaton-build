(ns automaton-build.utils.keyword
  "Utility functions for keywords."
  (:require
   [clojure.string :as str]))

(defn keywordize
  "Change string to appropriate clojure keyword"
  [s]
  (-> (name s)
      str/lower-case
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword)))

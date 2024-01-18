(ns automaton-build.utils.comparators "Gathering useful specific comparators")

(defn comparator-kw-symbol
  "Comparator to sort keywords and symbol.
  First start with keywords, then symbols.
  Keywords are sorted alphabetically, symbols also"
  [key1 key2]
  (cond
    (and (keyword? key1) (not (keyword? key2))) true
    (and (keyword? key2) (not (keyword? key1))) false
    :else (not (pos? (compare key1 key2)))))

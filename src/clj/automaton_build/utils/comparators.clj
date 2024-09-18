(ns automaton-build.utils.comparators
  "Gathering useful specific comparators"
  (:require
   [automaton-build.utils.regexp :as build-utils-regexp]
   [clojure.string               :as str]))

(defn comparator-kw-symbol
  "Comparator to sort keywords and symbol.
  First start with keywords, then symbols.
  Keywords are sorted alphabetically, symbols also"
  [key1 key2]
  (cond
    (and (keyword? key1) (not (keyword? key2))) true
    (and (keyword? key2) (not (keyword? key1))) false
    :else (not (pos? (compare key1 key2)))))

(defn comparable-val
  "Returns hash value and makes sure `s` is a string and removes all whitespaces and empty space chars."
  [s]
  (-> s
      str
      (str/replace (build-utils-regexp/all-white-spaces) "")
      hash))

(defn compare-file-change
  "Return true if file at `filename` read with `reader` is the same as `content`.
   Params:
   * `reader` fn to read a file
   * `filename` path where the file to read is
   * `content` content to compare read file with
   * `header` (optional) header that could be added to file"
  ([reader filename content header]
   (let [previous-content (some-> filename
                                  reader)
         content-with-header
         (if (str/blank? (str header)) content (str (with-out-str (println header)) content))
         new-content (if (and (string? previous-content)
                              (string? header)
                              (.contains (str previous-content) (str header)))
                       content-with-header
                       content)]
     (and (some? previous-content)
          (= (comparable-val previous-content) (comparable-val new-content)))))
  ([reader filename content] (compare-file-change reader filename content nil)))

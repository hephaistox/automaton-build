(ns automaton-build.code.forbidden-words
  "Search for some (forbidden) keywords in the project code."
  (:require
   [clojure.string :as str]))

(defn coll-to-alternate-in-regexp
  "Turns `coll` - a collection of strings or patterns - like (\"a\" \"b\") to \"(a|b)\""
  [coll]
  (if (empty? coll)
    nil
    (->> coll
         (map str)
         (str/join "|")
         (format "^.*(%s).*$")
         re-pattern)))

(defn forbidden-words-matches
  "Creates the list of matches of lines of `file-content` matching the `regexp`.

  Returns the matches

  Params:
  * `regexp` regexp (with groups) of strings to search
  * `clj-repo`"
  [regexp file-content]
  (when regexp
    (let [file-lines (str/split-lines file-content)]
      (when-not (re-find #":heph-ignore\s*\{[^\}]*:forbidden-words" (first file-lines))
        (->> file-lines
             (map (fn [line] (re-find regexp line)))
             (filter (comp not empty?))
             (mapv first))))))

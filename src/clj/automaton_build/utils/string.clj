(ns automaton-build.utils.string
  "String manipulation usable both in clj and cljs"
  (:require
   [clojure.edn :as edn]))

(defn remove-last-character
  "Remove the last character of a string"
  [s]
  (let [s (str s)] (subs s 0 (max 0 (- (count s) 1)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn remove-first-character
  "Remove in `s` the first character if it mathes `char`
  Params:
  * `s` string to test
  * `char` character to remove from `s` if it is the first char"
  [s char]
  (if (= char (first s)) (subs s 1) s))

(defn remove-first-last-character
  "Remove the first and last character of a string"
  [s]
  (let [s (str s)
        count-s (count s)]
    (if (< 2 count-s) (subs s 1 (max 0 (- (count s) 1))) "")))

(def ellipsis "...")

(defn- shrink-string-from-end
  [s prefix suffix limit]
  (apply str
         (concat prefix
                 (take (- limit (count ellipsis) (count prefix) (count suffix))
                       s)
                 ellipsis
                 suffix)))

(defn limit-length
  "Limit the length of the string
  Params:
  * `s` string to limit
  * `limit` maximum numbers of character of the resulting string, with prefix and suffix included, with an ellipsis of string s if necessary
  * `on-ellipsis` a function executed when the ellipsis is done"
  ([s limit] (limit-length s limit nil nil identity))
  ([s limit prefix suffix on-ellipsis]
   (let [line (str prefix s suffix)]
     (if (<= (count line) limit)
       line
       (do (on-ellipsis s) (shrink-string-from-end s prefix suffix limit))))))

(defn- shrink-string-from-beginning
  [s prefix suffix limit]
  (apply str
         (concat prefix
                 ellipsis
                 (subs
                  s
                  (- (count s)
                     (- limit (count prefix) (count ellipsis) (count suffix))))
                 suffix)))

(defn fix-length
  "Fix the length of the string, meaning it will be add ellipsis at the beginning of the string if needed, or add white spaces otherwise
  Params:
  * `s` string that will be shrinked
  * `limit` maximum numbers of character of the resulting string, with prefix and suffix included, with an ellipsis of string s if necessary
  * `prefix` string to add before the string (copied as is)
  * `suffix` string to add after the string (copied as is)
  * `on-ellipsis` a function executed when the ellipsis is done"
  [s limit prefix suffix]
  (let [line (str prefix s suffix)]
    (if (<= (count line) limit)
      (apply str line (repeat (- limit (count line)) " "))
      (shrink-string-from-beginning s prefix suffix limit))))

(defn remove-trailing-character
  "Remove last character if it is matching char
  Params:
  * `s` string
  * `char` a character to compare to last character of `s`"
  [s char]
  (if (= char (last s))
    (subs s
          0
          (-> s
              count
              dec))
    s))

(defn str->map [map-in-str] (edn/read-string map-in-str))

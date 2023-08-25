(ns automaton-build.adapters.string
  "Utils for string management")

(defn remove-last-character
  "Remove the last character of a string"
  [s]
  (subs (str s) 0 (max 0 (- (count s)
                            1))))

(def ellipsis "...")

(defn limit-length
  "Limit the length of the string
  Params:
  * `s` string to limit
  * `limit` maximum numbers of character of the resulting string, with prefix and suffix included, with an ellipsis of string s if necessary
  * `on-ellipsis` a function executed when the ellipsis is done"
  ([s limit]
   (limit-length s limit nil nil identity))
  ([s limit prefix suffix on-ellipsis]
   (let [line (str prefix s suffix)]
     (if (<= (count line) limit)
       line
       (do
         (on-ellipsis s)
         (apply str
                (concat prefix
                        (take (- limit
                                 (count ellipsis)
                                 (count prefix)
                                 (count suffix))
                              s)
                        ellipsis
                        suffix)))))))

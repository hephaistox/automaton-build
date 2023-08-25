(ns automaton-build.adapters.code-formatter
  "Format code
  Proxy to zprint"
  (:require
   [clojure.string :as str]

   [zprint.core :as zp]

   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.time :as time]))

(defn format-content
  "Format the data structure"
  [content]
  (zp/zprint-str content))

(defn format-file
  "Format the `clj` or `edn` file
  Params:
  * `filename` to format
  * `header` (optional) is written at the top of the file"
  ([filename header]
   (let [format-content (zp/zprint-file-str (slurp filename)
                                            nil)]
     (files/spit-file filename
                      (apply str [(when-not (str/blank? header)
                                    (println-str ";;" header (time/now-str)))
                                  format-content]))))
  ([filename]
   (format-file filename nil)))

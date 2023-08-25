(ns automaton-build.adapters.markdown
  "Adapter to markdown language"
  (:require
   [clojure.string :as str]

   [automaton-core.adapters.files :as files]))

(defn create-md
  "Build the markdown file
  Params:
  * `filename` is the name of the md file to store`
  * `content` is the string to spit"
  [filename content]
  (let [formatted-content (if (sequential? content)
                            (str/join "\n" content)
                            (str content))]
    (files/spit-file filename
                     formatted-content)))

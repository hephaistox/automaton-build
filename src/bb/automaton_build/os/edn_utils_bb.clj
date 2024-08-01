(ns automaton-build.os.edn-utils-bb
  "Adapter to read an edn file."
  (:require
   [automaton-build.os.file :as build-file]
   [clojure.edn             :as edn]))

(defn read-edn
  "Read file which name is `edn-filename`."
  [edn-filename]
  (-> (build-file/read-file edn-filename)
      edn/read-string))

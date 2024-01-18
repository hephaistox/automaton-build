(ns automaton-build.os.json
  "Everything about json manipulation"
  (:require
   [automaton-build.os.files :as build-files]
   [automaton-build.log :as build-log]
   [clojure.data.json :as json]
   [clojure.java.io :as io]))

(defn read-file
  [filepath]
  (try (json/read-str (build-files/read-file filepath))
       (catch Exception e
         (build-log/error-exception e)
         (build-log/error-data {:path filepath} "Loading json file has failed ")
         nil)))

(defn write-file
  [target-path content]
  (try (with-open [w (io/writer target-path)]
         (json/write content w :indent true :escape-slash false))
       (catch Exception e
         (build-log/error-exception e)
         (build-log/error-data {:target-path target-path
                                :content content
                                :e e}
                               "Writing json file has failed")
         nil)))

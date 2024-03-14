(ns automaton-build.os.json
  "Everything about json manipulation"
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]
   [automaton-build.utils.comparators :as build-utils-comparators]
   [babashka.json :as json]))

(defn read-file
  [filepath]
  (try (json/read-str (build-files/read-file filepath) {:key-fn identity})
       (catch Exception e
         (build-log/error-exception e)
         (build-log/error-data {:path filepath} "Loading json file has failed ")
         nil)))

(defn write-file
  [filename content]
  (try (build-files/spit-file
        filename
        (json/write-str content)
        nil
        (partial build-utils-comparators/compare-file-change read-file))
       (catch Exception e
         (build-log/error-exception e)
         (build-log/error-data {:target-path filename
                                :content content
                                :e e}
                               "Writing json file has failed")
         nil)))

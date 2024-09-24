(ns automaton-build.os.json
  "Everything about json manipulation"
  (:require
   [automaton-build.os.file :as build-file]
   [cheshire.core           :as json]))

(defn read-file
  "Reads a file which name is `filepath`
  Returns map with keys :raw-content, :json if file can be read. Otherwise :exception and :invalid? set to true"
  [filepath]
  (let [{:keys [exception raw-content]
         :as content}
        (build-file/read-file filepath)]
    (if (nil? exception)
      (try (assoc content :json (json/parse-string raw-content))
           (catch Exception e
             {:exception e
              :path filepath}))
      content)))

(defn write-file
  [filename content]
  (try (build-file/write-file filename (json/generate-string content {:pretty true}))
       (catch Exception e
         {:exception e
          :target-path filename
          :content content})))

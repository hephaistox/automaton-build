(ns automaton-build.configuration.edn-read
  "Specific read edn for configuration
  We cannot use edn-utils there, as that namespace would create a cycle dependancy"
  (:require
   [automaton-build.log :as build-log]
   [babashka.fs         :as fs]
   [clojure.edn         :as edn]))

(defn read-edn
  "Read the `.edn` file,
  Params:
  * `edn-filename` name of the edn file to load
  Errors:
  * throws an exception if the file is not found
  * throws an exception if the file is a valid edn
  * `file` could be a string representing the name of the file to load
  or a (io/resource) object representing the name of the file to load"
  [edn-filename]
  (let [edn-filename (when edn-filename (str (fs/absolutize edn-filename)))
        edn-content (try (slurp edn-filename)
                         (catch Exception _
                           (build-log/warn-format "Unable to load the file `%s`" edn-filename)
                           nil))]
    (when edn-content
      (build-log/trace "Load file:" edn-filename)
      (try (edn/read-string edn-content)
           (catch Exception _
             (build-log/warn-format "File `%s` is not a valid edn" edn-filename)
             nil)))))

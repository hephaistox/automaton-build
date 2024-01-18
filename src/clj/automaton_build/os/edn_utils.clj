(ns automaton-build.os.edn-utils
  "Adapter to read an edn file"
  (:require
   [automaton-build.code-helpers.formatter :as build-code-formatter]
   [automaton-build.os.files :as build-files]
   [automaton-build.log :as build-log]
   [clojure.edn :as edn]))

(defn parse-edn
  "Parse an `edn` string,
  Params:
  * `edn-filename` name of the edn file to load"
  [s]
  (try (edn/read-string s)
       (catch Exception e
         (build-log/warn-format "Unable to parse string `%s`" s)
         (build-log/trace-exception e)
         nil)))

(defn read-edn
  "Read the `.edn` file,
  Design decision:
  * It is the caller responsability to write this action in a file

  Params:
  * `edn-filename` name of the edn file to load"
  [edn-filename]
  (try (let [edn-filename (build-files/absolutize edn-filename)
             edn-content (build-files/read-file edn-filename :silently)]
         (parse-edn edn-content))
       (catch Exception e
         (build-log/error-exception (ex-info (format "File `%s` is not an edn."
                                                     edn-filename)
                                             {:caused-by e
                                              :file-name edn-filename}))
         nil)))

(defn spit-edn
  "Spit the `content` in the edn file called `deps-edn-filename`.
  If any, the header is added at the top of the file
  Params:
  * `edn-filename` Filename
  * `content` What is spitted
  * `header` the header that is added to the content, followed by the timestamp - is automatically preceded with ;;
  Return the content of the file"
  ([edn-filename content header]
   (try
     (when (nil? edn-filename)
       (throw (Exception. "Impossible to save the file due to empty filename")))
     (let [content (doall content)
           previous-content (some-> edn-filename
                                    build-files/is-existing-file?
                                    read-edn)]
       (cond
         (and (some? previous-content)
              (= (hash previous-content) (hash content)))
         (build-log/debug-format
          "Spit of file `%s` skipped, as it is already up to date:"
          edn-filename)
         (some? content) (do (build-log/debug-format
                              "Spit of file `%s` is updating with new content."
                              edn-filename)
                             (build-files/spit-file edn-filename content header)
                             (build-code-formatter/format-file edn-filename))
         :else (build-log/debug "Content is empty"))
       content)
     (catch Exception e
       (build-log/error-data {:deps-edn-filename edn-filename
                              :exception e
                              :content content}
                             (format "Impossible to update the `%s` file."
                                     edn-filename))
       nil)))
  ([edn-filename content] (spit-edn edn-filename content nil)))

(defn create-tmp-edn
  "Create a temporary file with edn extension
  Params:
  * `filename` relative filename to save"
  [filename]
  (-> (build-files/create-temp-dir)
      (build-files/create-file-path filename)))

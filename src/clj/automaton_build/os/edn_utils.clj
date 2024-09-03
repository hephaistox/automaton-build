(ns automaton-build.os.edn-utils
  "Adapter to read an edn file"
  (:require
   [automaton-build.code-helpers.formatter :as build-code-formatter]
   [automaton-build.log                    :as build-log]
   [automaton-build.os.files               :as build-files]
   [clojure.edn                            :as edn]))

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
  "Read the `.edn` file.

  Design decision:
  * For now it is the caller responsability to write this action in a log (To be refactored)
  Params:
  * `edn-filename` name of the edn file to load"
  [edn-filename]
  (try (let [edn-filename (build-files/absolutize edn-filename)
             edn-content (build-files/read-file edn-filename :silently)]
         (parse-edn edn-content))
       (catch Exception e
         (build-log/error-exception (ex-info (format "File `%s` is not an edn." edn-filename)
                                             {:caused-by e
                                              :file-name edn-filename}))
         nil)))

(defn compare-edn [filename content _header] (= (read-edn filename) content))

(defn spit-edn
  "Spit the `content` in the edn file called `deps-edn-filename`.
  If any, the header is added at the top of the file
  Params:
  * `edn-filename` Filename
  * `content` What is spitted
  * `header` the header that is added to the content, followed by the timestamp - is automatically preceded with ;;
  Return the content if spit is successful, false if there is no change and nil if there is an error."
  ([edn-filename content header]
   (try (when (nil? edn-filename)
          (throw (ex-info "Impossible to save the file due to empty filename"
                          {:edn-filename edn-filename})))
        (let [spit-res (build-files/spit-file edn-filename
                                              content
                                              (when header (str ";; " header))
                                              compare-edn)]
          (build-code-formatter/format-file edn-filename)
          spit-res)
        (catch Exception e
          (build-log/error-data {:deps-edn-filename edn-filename
                                 :exception e
                                 :content content}
                                (format "Impossible to update the `%s` file." edn-filename))
          nil)))
  ([edn-filename content] (spit-edn edn-filename content nil)))

(defn create-tmp-edn
  "Create a temporary file with edn extension
  Params:
  * `filename` relative filename to save"
  [filename]
  (-> (build-files/create-temp-dir)
      (build-files/create-file-path filename)))

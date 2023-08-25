(ns automaton-build.adapters.edn-utils
  "Edn file manipulation"
  (:require
   [clojure.edn :as edn]

   [automaton-build.adapters.code-formatter :as code-formatter]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.log :as log]
   [automaton-build.adapters.uuid :as uuid]
   [automaton-build.env-setup :as env-setup]))

(defn is-clojure-like-file
  "Returns true if the file's extension is clojure like"
  [filename]
  (boolean
   (re-find #"\.clj[sc]?$"
            (or filename
                ""))))

(defn read-edn
  "Read the `.edn` file,
  Params:
  * `edn-filename` name of the edn file to load
  * `loader-fn` (Optional, default: files/read-file) a function returning the content of the file

  Errors:
  * throws an exception if the file is not found
  * throws an exception if the file is a valid edn
  * `file` could be a string representing the name of the file to load
  or a (io/resource) object representing the name of the file to load"
  ([edn-filename loader-fn]
   (let [edn-filename (files/absolutize edn-filename)
         _ (log/trace "Load file:" edn-filename)
         edn-content (try
                       (loader-fn edn-filename)
                       (catch Exception e
                         (throw (ex-info (format "Unable to load the file `%s`" edn-filename)
                                         {:caused-by e
                                          :file-name edn-filename}))))]
     (try
       (edn/read-string edn-content)
       (catch Exception e
         (throw (ex-info (format "File `%s` is not an edn" edn-filename)
                         {:caused-by e
                          :file-name edn-filename}))))))
  ([edn-filename]
   (read-edn edn-filename files/read-file)))

(defn read-edn-or-nil
  "Read the `.edn` file,
  * return nil if the file does not exist or is invalid
  * `file` could be a string representing the name of the file to load
  or a (io/resource) object representing the name of the file to load"
  ([edn-file-name loader-fn]
   (try
     (read-edn edn-file-name loader-fn)
     (catch Exception _
       nil)))
  ([edn-file-name]
   (read-edn-or-nil edn-file-name slurp)))

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
     (log/trace "Spit edn file:" edn-filename)
     (files/spit-file edn-filename
                      content)
     (code-formatter/format-file edn-filename
                                 header)
     content
     (catch Exception e
       (throw (ex-info "Impossible to update the .edn file"
                       {:deps-edn-filename edn-filename
                        :exception e
                        :content content})))))
  ([deps-edn-filename content]
   (spit-edn deps-edn-filename content nil)))

(defn update-edn-content
  "Update the edn file content with the `params-to-merge` map
  Returns the content of the file
  Params:
  * `edn-filename` the name of the file to update
  * `update-fn` function updating the content (fn [edn-content] (assoc edn-content ...))
  * `header` optional is a string added at the top of the file
  Note: the content will be formatted thanks to `build.adapters.code-formatter`
  "
  ([edn-filename update-fn header]
   (let [bb-config (read-edn edn-filename)]
     (spit-edn edn-filename
               (update-fn bb-config)
               header)))
  ([edn-filename update-fn]
   (update-edn-content edn-filename update-fn nil)))

(defn spit-edn-or-file
  "Spit the file as an edn if it is a clojure file, or spit it with no modification otherwise"
  [filename rendered-content]
  (if (is-clojure-like-file filename)
    (spit-edn filename rendered-content)
    (files/spit-file filename rendered-content)))

(defn create-tmp-edn
  "Create a temporary file directory string with edn extension"
  []
  (let [edn-file (files/create-file-path (get-in env-setup/env-setup
                                                 [:log :spitted-edns])
                                         (str (uuid/time-based-uuid) ".edn"))]
    (files/create-dirs (files/extract-path edn-file))
    edn-file))

(defn spit-in-tmp-file
  "Spit the data given as a parameter to a temporary file which adress is given
  This function has a trick to print exception and its stacktrace
  Params:
  * `data` the data to spit"
  [data]
  (let [filename (create-tmp-edn)
        ;; Important to print exception properly
        formatted-data (code-formatter/format-content data)]
    (files/spit-file filename formatted-data)
    (format " See file `%s` for details"
            (files/absolutize filename))))

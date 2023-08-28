(ns automaton-build.adapters.build-config
  "Manage `build-config.edn` file"
  (:require
   [automaton-core.adapters.edn-utils :as edn-utils]
   [automaton-core.adapters.files :as files]))

(def build-config-filename
  "build_config.edn")

(defn search-for-build-config
  "Scan the directory to find build-config files, starting in the current directory
  Useful to discover applications
  Search in the local directory, useful for application repo
  and in subdir, useful for monorepo
  Params:
  * none
  Returns the list of directories with `build_config.edn` in it"
  []
  (->> (files/search-files ""
                           (str "**" build-config-filename))
       flatten
       (filter (comp not nil?))))

(defn spit-build-config
  "Spit a build config file
  Params:
  * `app-dir` where to store the build_config file
  * `content` to spit
  * `msg` (optional) to add on the top of the file"
  ([app-dir content msg]
   (let [filename (files/create-file-path app-dir
                                          build-config-filename)]
     (edn-utils/spit-edn filename
                         content
                         msg)
     filename))
  ([app-dir content]
   (spit-build-config app-dir content nil)))

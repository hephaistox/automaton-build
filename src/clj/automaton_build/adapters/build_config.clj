(ns automaton-build.adapters.build-config
  "Manage `build-config.edn` file"
  (:require
   [automaton-build.adapters.edn-utils :as edn-utils]
   [automaton-build.adapters.files :as files]))

(def build-config-filename
  "build_config.edn")

(defn one-or-multiple-apps
  "Checks if the build_config informs about other apps. If so it returns them else returns the provided edn file."
  [app-edn-file]
  (if-let [edn-map (edn-utils/read-edn app-edn-file)]
    (if (:multiple? edn-map)
      (:apps edn-map)
      app-edn-file)
    app-edn-file))

(defn search-for-build-config
  "Scan the directory to find build-config files, starting in the current directory
  Useful to discover applications
  Search in the local directory, useful for application repo
  and in subdir, useful for monorepo
  Params:
  * none
  Returns the list of directories with `build_config.edn` in it"
  []
  (->> (let [subdir (conj (files/list-subdir "")
                          "")]
         (for [path subdir]
           (let [bb-config-filename (files/create-file-path path
                                                            build-config-filename)]
             (when (files/is-existing-file? bb-config-filename)
               (one-or-multiple-apps bb-config-filename)))))
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

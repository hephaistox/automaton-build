(ns automaton-build.app.bb-edn
  "Adapter for `bb.edn`"
  (:refer-clojure :exclude [slurp spit])
  (:require
   [automaton-build.app.deps-edn :as build-deps-edn]
   [automaton-build.log :as build-log]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files :as build-files]))

(def bb-edn-filename
  "Should not be used externally except in test namespaces"
  "bb.edn")

(defn bb-edn-filename-fullpath
  "Return the full path of the bb.edn file"
  [app-dir]
  (build-files/create-file-path app-dir bb-edn-filename))

#_{:clj-kondo/ignore [:redefined-var]}
(defn slurp
  "Returns the bb-edn file content"
  [app-dir]
  (let [filepath (-> app-dir
                     build-files/absolutize
                     bb-edn-filename-fullpath)
        bb-edn (build-edn-utils/read-edn filepath)]
    (if (and filepath bb-edn)
      bb-edn
      (build-log/error-format
       "Are you sure directory `%s` is an app, no valid bb task in it"))))


#_{:clj-kondo/ignore [:redefined-var]}
(defn spit
  "Update the `bb-edn` content with the mono file with the file parameter, keep :tasks and :init keys and refresh aliases with tasks content

  Params:
  * `app-dir`
  * `bb-edn`
  * `deps-edn`"
  [app-dir bb-edn deps-edn]
  (if-let [bb-edn (->> (build-deps-edn/get-bb-deps deps-edn)
                       (assoc bb-edn :deps))]
    (build-edn-utils/spit-edn (-> app-dir
                                  build-files/absolutize
                                  bb-edn-filename-fullpath)
                              bb-edn
                              "The file is updated automatically")
    (build-log/error
     "Can't proceed with update of `bb.edn` as `:bb-deps` in `deps.edn` is empty")))

(defn tasks
  "Return the tasks from the bb-edn file in parameter"
  [bb-edn]
  (->> (:tasks bb-edn)
       keys
       (remove #(keyword? %))
       sort
       vec))

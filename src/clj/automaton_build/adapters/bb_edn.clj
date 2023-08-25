(ns automaton-build.adapters.bb-edn
  "Adapter for `bb.edn`"
  (:require
   [automaton-build.adapters.edn-utils :as edn-utils]
   [automaton-build.tasks :as tasks]
   [automaton-build.adapters.files :as files]))

(def bb-edn-filename
  "Should not be used externally except in test namespaces"
  "bb.edn")

(defn update-bb-edn
  "Update the `bb-edn` with the mono file with the file parameter, keep :tasks and :init keys and refresh aliases with tasks content
  Params:
  * `bb-edn-dir` name of the dir where the bb.edn is
  * `tasks` list of tasks to push in the `bb.edn` file"
  [bb-edn-dir tasks]
  (let [bb-edn-filename (files/create-file-path bb-edn-dir bb-edn-filename)
        bb-edn (edn-utils/read-edn bb-edn-filename)
        updated-bb-edn (assoc bb-edn
                              :tasks (merge (select-keys (:tasks bb-edn)
                                                         [:init :requires])
                                            (tasks/create-bb-tasks tasks)))]
    (edn-utils/spit-edn bb-edn-filename
                        updated-bb-edn)))

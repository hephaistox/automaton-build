(ns automaton-build.tasks.registry.specific-task
  "Adapter to the specific_task_registry.edn
  This namespace is used to start the cli, so no log or files namespaces are used.
  So there are not used before they got intialized"
  (:require
   [babashka.fs    :as fs]
   [clojure.edn    :as edn]
   [clojure.string :as str]))

(def ^:private specific-task-registry "specific_task_registry.edn")

(defn read-specific-tasks
  "Read the map of tasks definitions specific for that customer map"
  [app-dir]
  (let [filename
        (str (if (str/blank? app-dir) "." app-dir) "/" specific-task-registry)]
    (when (fs/exists? filename)
      (some-> filename
              slurp
              edn/read-string))))

(comment
  (read-specific-tasks ""))

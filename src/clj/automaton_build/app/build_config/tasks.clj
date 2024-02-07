(ns automaton-build.app.build-config.tasks)

(defn task-names->tasks-map
  "Returns the list of tasks updated with an empty map for all tasks in `tasks`, existing tasks config are not modified"
  [tasks]
  (->> tasks
       (mapv (fn [k] [(keyword k) {}]))
       (into {})))

(defn tasks-names
  "Return the list of tasks (kws collection) setup in the build_config.edn file
  Params:
  * `build-config`"
  [build-config]
  (-> (get build-config :tasks)
      keys
      vec))

(defn add-tracked-tasks
  "Adds to the task related keywords from build-config."
  [tasks-map]
  (->> tasks-map
       (map (fn [[k v]] [k
                         (assoc v
                                :build-config-task-kws
                                (->> (concat [k] (:wk-tasks v))
                                     (mapv keyword)
                                     vec))]))))

(defn update-build-config-tasks
  "Updates build-config with new tasks."
  [build-config new-tasks]
  (->> (get build-config :tasks)
       (merge (task-names->tasks-map new-tasks))
       (assoc build-config :tasks)))

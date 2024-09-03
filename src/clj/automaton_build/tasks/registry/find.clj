(ns automaton-build.tasks.registry.find "Function to find elemnts in the registry")

(defn search-task
  [registry task-name]
  (->> (some-> task-name
               symbol)
       (get registry)))

(defn task-map
  "Return the map describing the task called `task-name`, as defined in the registry
  Params:
  * `registry` as build with `build` fn
  * `task-name` string, name of the task to look at"
  [registry task-name]
  (search-task registry task-name))

(defn task-selection
  "Transform a list of task names into a list of task maps"
  [task-registry tasks-names]
  (->> tasks-names
       (mapv (fn [task-name] (get (into {} task-registry) (symbol task-name))))
       (filterv some?)))

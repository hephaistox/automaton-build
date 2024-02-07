(ns automaton-build.app.bb-edn.task-updater
  "Update the tasks in the bb edn file

  Note that:
  * It is cust-app responsibility to tell what tasks should be presented to the user or not
  * cust-app can choose to use all these tasks, and remove a selection
  * or cust-app can select only some all-tasks
  * Whatever that choice, all unknown tasks from this list are left unchanged by `automaton-build`, so cust-app can have its own set of specific tasks
  * The source of truth of tasks are here, what's in `bb.edn` is just a copy necessary for bb startup")

(defn- registry-item-to-bb-task
  "Create a bb task from a bb registry item
  Tasks are referencing the real function to make it easier for developer workflow: so searching the entry point can be started in the bb.edn file and can \"jump to definition\"
  Params:
  * `registry-bb-task`"
  [{:keys [doc task-fn wk-tasks]
    :as _registry-bb-task}]
  {:doc doc
   :task (->> (concat ['execute-build-app-task (list 'quote task-fn)]
                      (when wk-tasks (mapv #(list 'quote %) wk-tasks)))
              (apply list))})

(defn- registry-items-to-bb-task
  "Add tasks from `registry-tasks` to `bb-edn-tasks`
  Params:
  * `registry-tasks` List of tasks to add, coming from the registry, and ready to be inserted
  * `bb-edn-tasks` content of the `bb.edn` file"
  [registry-tasks bb-edn-tasks]
  (->> registry-tasks
       (map (fn [[k v]] [k (registry-item-to-bb-task v)]))
       (into {})
       (merge bb-edn-tasks)))

(defn- remove-bb-tasks
  [tasks-to-remove bb-edn-tasks]
  (let [bb-edn-tasks (into {} bb-edn-tasks)]
    (->> tasks-to-remove
         (map symbol)
         (apply dissoc bb-edn-tasks))))

(defn- update-bb-tasks*
  "Update the tasks in the `bb-content` with the ones coming from the registry
  All tasks in the registry but excluded are removed,
  The other tasks in the registry are upserted
  The rest of the tasks present are untouched, it allows exotic tasks to be added there

  Return the updated bb-content
  Params:
  * `registry-bb-tasks`
  * `exclude-tasks`
  * `bb-content`"
  [registry-bb-tasks exclude-tasks bb-content]
  (-> bb-content
      (update :tasks (partial registry-items-to-bb-task registry-bb-tasks))
      (update :tasks (partial remove-bb-tasks exclude-tasks))))

(defn update-bb-tasks
  "Update the bb tasks in the bb-edn of `app`
   Return the updated app.

  Params:
  * `app` app map
  * `task-registry` registry of tasks to build
  * `tasks-to-remove` tasks to remove from the file"
  [app task-registry tasks-to-remove]
  (update app :bb-edn (partial update-bb-tasks* task-registry tasks-to-remove)))

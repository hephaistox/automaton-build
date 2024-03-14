(ns automaton-build.tasks.launcher.app-data
  "Before a task execution, gather all data needed by the task to provide its app-data"
  (:require
   [automaton-build.app.bb-edn :as build-bb-edn]
   [automaton-build.app.build-config :as build-build-config]
   [automaton-build.app.build-config.tasks :as build-config-tasks]
   [automaton-build.app.deps-edn :as build-deps-edn]
   [automaton-build.app.package-json :as build-package-json]
   [automaton-build.code-helpers.compiler.shadow :as build-compiler-shadow]
   [automaton-build.log :as build-log]
   [automaton-build.tasks.launcher.cli-task-opts :as build-tasks-cli-opts]
   [automaton-build.tasks.launcher.task :as build-launcher-task]
   [automaton-build.utils.map :as build-utils-map]))

(defn build-config-task-data
  "Get task data from `build-config`"
  [build-config task-kw]
  (-> (get build-config :tasks)
      (select-keys (map keyword task-kw))
      vals))

(defn build-config-data
  "Gathers all data from build-config configurations, which contains data on tasks (specific-task-registry, tasks.registry.common), defaults from build-config-schema and data from build_config.edn"
  [raw-build-config
   task-name
   mandatory-tasks
   tasks-schema
   build-config-task-kws
   shared]
  (let [build-config
        (->> (build-config-tasks/update-build-config-tasks raw-build-config
                                                           mandatory-tasks)
             (build-build-config/build-config-default-values tasks-schema))
        build-config-task-data
        (or (build-config-task-data build-config build-config-task-kws) {})
        build-config-shared-data (-> (get build-config :task-shared)
                                     (build-utils-map/select-keys*
                                      (map keyword shared)))]
    (when (nil? build-config-task-data)
      (build-log/warn-format
       "`build-config.edn` does not contain any value for a mandatory task [:tasks %s]"
       task-name)
      nil)
    (apply merge build-config-shared-data build-config-task-data)))

(defn app-data
  "Gather data describing the application stored in `app-dir`
  Params:
  * `app-dir` is where all the files will be searched"
  [app-dir]
  (build-log/debug-format "Build app data based on directory `%s`" app-dir)
  (let [build-config (build-build-config/read-build-config app-dir)
        app-name (get build-config :app-name)]
    {:raw-build-config build-config
     :app-dir app-dir
     :app-name app-name
     :bb-edn (build-bb-edn/slurp app-dir)
     :shadow-cljs (build-compiler-shadow/load-shadow-cljs app-dir)
     :package-json (build-package-json/load-package-json app-dir)
     :deps-edn (build-deps-edn/slurp app-dir)}))

(defn task-app-data
  ([app-dir task-name cli-args]
   (task-app-data app-dir task-name cli-args nil nil))
  ([app-dir task-name cli-args task-map* cli-opts*]
   (let [{:keys [app-dir
                 app-name
                 raw-build-config
                 bb-edn
                 deps-edn
                 shadow-cljs
                 package-json]
          :as _app}
         (app-data app-dir)
         {:keys [task-map task-registry tasks-schema mandatory-tasks]
          :as tasks}
         (->> (build-config-tasks/tasks-names raw-build-config)
              (build-launcher-task/build app-dir task-name))
         {:keys [build-config-task-kws shared task-cli-opts-kws]} (or task-map*
                                                                      task-map)
         {:keys [options]
          :as cli-opts}
         (or cli-opts*
             (build-tasks-cli-opts/cli-opts task-cli-opts-kws cli-args))]
     (if (or (not (build-tasks-cli-opts/are-cli-opts-valid?
                   cli-opts
                   "That arguments are not compatible"))
             (not (build-tasks-cli-opts/mandatory-option-present?
                   cli-opts
                   task-cli-opts-kws))
             (empty? tasks))
       (do (build-log/warn "Can't continnue creating app-data") nil)
       (let [build-config-data (build-config-data raw-build-config
                                                  task-name
                                                  mandatory-tasks
                                                  tasks-schema
                                                  build-config-task-kws
                                                  shared)]
         (apply merge
                build-config-data
                options
                {:cli-args cli-args
                 :cli-opts cli-opts}
                {:task-name task-name
                 :task-map task-map
                 :task-registry task-registry}
                {:app-name app-name
                 :app-dir app-dir
                 :shadow-cljs shadow-cljs
                 :package-json package-json
                 :deps-edn deps-edn
                 :bb-edn bb-edn}))))))

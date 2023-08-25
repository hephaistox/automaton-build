(ns automaton-build.core
  "Build of the application. Useful for monorepo and customer applications"
  (:require
   [clojure.string :as str]

   [automaton-build.adapters.bb-edn :as bb-edn]
   [automaton-build.adapters.build-config :as build-config]
   [automaton-build.adapters.cicd :as cicd]
   [automaton-build.adapters.edn-utils :as edn-utils]
   [automaton-build.adapters.log :as log]
   [automaton-build.apps :as apps]
   [automaton-build.cli-params :as cli-params]
   [automaton-build.exit-codes :as exit-codes]
   [automaton-build.monorepo.deps-edn :as monorepo-deps]
   [automaton-build.tasks :as tasks]))

(defn get-task
  "Retrieve the data describing the task"
  [tasks task-name]
  (let [task-data (get (into {} tasks)
                       task-name)
        {:keys [exec-task]} task-data]
    (when-not task-data
      (throw (ex-info "Don't know that task"
                      {:tasks tasks
                       :task-name task-name})))

    (when-not exec-task
      (throw (ex-info "Don't know how to execute that task"
                      {:tasks-data task-data
                       :vals vals})))
    task-data))

(defn update-bb-edn-with-tasks
  "Update the `bb.edn` file with tasks
  Params:
  * `tasks` to publish in `bb.edn`, is a map"
  [tasks]
  (bb-edn/update-bb-edn ""
                        (tasks/create-bb-tasks tasks)))

(defn create-apps
  "Create the apps list from the list of `build-config-filenames`
  Params:
  * none
  Return the `apps` data"
  []
  (apps/build-apps (build-config/search-for-build-config)))

(defn run
  "Transform command line parameters to execution of the tasks
  Params:
  * `apps` active for that cli, it could be monorepo or one and only one app
  * `cli-params` is what given to the cli as parameters
  * `tasks` ."
  [apps
   {{:keys [task-name first-param second-param]
     :as cli-params} :cli-params}
   tasks]
  (try
    (let [task-name (str task-name)
          first-param (str first-param)
          second-param (str second-param)
          {:keys [exec-task cli-params-mode create-deps-at-startup?]} (get-task tasks
                                                                                task-name)
          task-params (cli-params/create-task-params apps
                                                     cli-params-mode
                                                     cli-params
                                                     #(System/exit exit-codes/invalid-argument))
          task-and-cli-params (-> task-params
                                  (assoc :first-param first-param
                                         :second-param second-param))]
      (when-not (cicd/is-cicd?)
        (update-bb-edn-with-tasks tasks))
      (log/info (apply str "Running task `" task-name "`"
                       (when-not (str/blank? first-param)
                         (str "with first-param parameter: `" first-param "`"))
                       (when-not (str/blank? second-param)
                         (str "with second-param parameter: `" second-param "`"))))
      (log/trace "task-params :" (edn-utils/spit-in-tmp-file task-and-cli-params))
      (when create-deps-at-startup?
        (monorepo-deps/build-save-deps-edn apps))
      (exec-task apps task-and-cli-params))
    (catch clojure.lang.ExceptionInfo e
      (log/fatal "Unexpected ExceptionInfo " (ex-message e))
      (log/trace (edn-utils/spit-in-tmp-file e))
      (System/exit exit-codes/unexpected-exception))
    (catch Exception e
      (log/fatal (str "Unexpected Exception:" (ex-message e)))
      (log/trace (edn-utils/spit-in-tmp-file e))
      (System/exit exit-codes/unexpected-exception))))

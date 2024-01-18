(ns automaton-build.tasks.registry
  "Build, validate and access the global registry for the automaton-build"
  (:require
   [automaton-build.app.build-config.tasks :as build-config-tasks]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.terminal-msg :as build-terminal-msg]
   [automaton-build.schema :as build-schema]
   [automaton-build.tasks.registry.common :as build-tasks-common]
   [automaton-build.tasks.registry.specific-task
    :as
    build-specific-task-registry]
   [automaton-build.tasks.workflow.workflow-to-task
    :as
    build-task-workflow-to-task]
   [automaton-build.utils.map :as build-utils-map]
   [automaton-build.utils.namespace :as build-namespace]
   [automaton-build.utils.string :as build-string]))

(def ^:private schema
  [:map-of
   :symbol
   [:map {:closed true}
    [:bb-edn-args {:optional true}
     :any]
    [:build-config-task-kws {:optional true}
     [:vector :keyword]]
    [:build-configs {:optional true}
     [:vector :some]]
    [:doc :string]
    [:group {:optional true}
     :keyword]
    [:hidden? {:optional true}
     [:or :symbol :boolean]]
    [:mandatory-config? {:optional true}
     :boolean]
    [:pf {:optional true}
     :keyword]
    [:shared {:optional true}
     [:vector :keyword]]
    [:step {:optional true}
     pos-int?]
    [:task-cli-opts-kws {:optional true}
     [:vector :keyword]]
    [:task-fn {:optional true}
     [:or fn? :symbol]]
    [:task-name :string]
    [:wk-tasks {:optional true}
     [:vector :symbol]]
    [:la-test {:optional true}
     [:map {:closed true}
      [:skip? {:optional true}
       :boolean]
      [:process-opts {:optional true}
       :map]
      [:expected-exit-code {:optional true}
       pos-int?]
      [:cmd {:optional true}
       [:vector :string]]]]]])

(defn- add-names
  "The name of the task as found as a key is copied in the val
  Params:
  * `tasks-map`"
  [tasks-map]
  (->> tasks-map
       (map (fn [[k v]] [k (assoc v :task-name (name k))]))
       (into {})))

(defn build-config-schema
  "Build the `build-config.edn` schema based on the task registry

  Params:
  * `task-registry` tasks as found in the registry"
  [task-registry]
  (->> task-registry
       (mapv (fn [[symbol {:keys [build-configs]}]]
               (vec (concat
                     [(keyword symbol) {:optional true}]
                     [(apply vector :map {:closed true} build-configs)]))))
       (concat [:map {:closed true}])
       vec))

(defn- add-default-la-test
  "Add the default `bb la` command to the tasks map
  Params:
  * `tasks-map`"
  [tasks-map]
  (->> tasks-map
       (map (fn [[k v]] [k
                         (if (nil? (get-in v [:la-test :cmd]))
                           (build-utils-map/deep-merge
                            v
                            {:la-test {:cmd ["bb" "heph-task" (name k)]}})
                           v)]))))

(defn- add-default-task-fn
  "Add the default task-fn to the tasks map
  Params:
  * `tasks-map`"
  [tasks-map]
  (->> tasks-map
       (map (fn [[k v]] [k
                         (if (nil? (:task-fn v))
                           (assoc v
                                  :task-fn
                                  (symbol (build-namespace/namespace-in-same-dir
                                           'automaton-build.tasks.error
                                           (-> (name k)
                                               (str "/exec")))))
                           v)]))))

(defn build
  "Build the global registry for that app.
  Remove deactivated tasks (i.e. the one with `:mandatory-config?` and no data in build-config.edn)
  It gather the data from the cust-app and common registry.
  Params:
  * `app-dir`
  * `setuped-tasks` task names"
  [app-dir setuped-tasks]
  (let [tasks-registry-map
        (->> (build-specific-task-registry/read-specific-tasks app-dir)
             (merge (build-tasks-common/tasks))
             (filter (fn [[k v]]
                       (or (not (:mandatory-config? v))
                           (some #(= (keyword k) %) setuped-tasks))))
             add-names
             add-default-la-test
             add-default-task-fn
             build-config-tasks/add-tracked-tasks
             build-task-workflow-to-task/update-registry-workflow-entries
             (into (sorted-map)))]
    (build-schema/valid? schema tasks-registry-map "task registry")
    (build-edn-utils/spit-edn "tmp/tasks.edn" tasks-registry-map)
    tasks-registry-map))

(defn task-names
  "Returns the list of symbols of tasks present in the registry.

  Params:
  * `task-registry`"
  [task-registry]
  (->> (keys task-registry)
       (mapv symbol)))

(defn print-tasks
  "Display on the terminal all tasks.

  Params:
  * `task-registry`
  * `app-data`"
  [task-registry app-data]
  (automaton-build.os.terminal-msg/println-msg
   "The following tasks are available:")
  (doseq [[task-name {:keys [hidden? doc]}] task-registry]
    (let [task-name (str task-name)
          hidden? (if (symbol? hidden?)
                    (build-namespace/symbol-to-fn-call hidden? app-data)
                    hidden?)]
      (when-not hidden?
        (automaton-build.os.terminal-msg/println-msg
         (build-string/fix-length task-name 30 nil " ")
         doc)))))

(defn not-mandatory
  "Filter the task registry to tasks with mandatory configuration,
  The other ones will be have default values.

  Params:
  * `task-registry`"
  [task-registry]
  (filter (comp not :mandatory-config? second) task-registry))

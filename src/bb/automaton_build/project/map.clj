(ns automaton-build.project.map
  "The project map gathers all useful informations for a project"
  (:require
   [automaton-build.project.config :as build-project-config]
   [automaton-build.project.deps   :as build-deps]))

(defn create-project-map
  "Creates a project map based on app in `app-dir`."
  [app-dir]
  {:app-dir app-dir})

(defn add-project-config
  "Adds project configuration file desc `project-config-filedesc`to the `project-map`."
  [{:keys [app-dir]
    :as project-map}]
  (let [project-config-filedesc (build-project-config/read-from-dir app-dir)]
    (assoc project-map
           :project-config-filedesc (when-not (:invalid?
                                               project-config-filedesc)
                                      project-config-filedesc)
           :app-name (get-in project-config-filedesc [:edn :app-name]))))

(defn add-deps-edn
  "Adds `:deps` key to to project map with the `deps.edn` file descriptor."
  [{:keys [app-dir]
    :as project-map}]
  (assoc project-map :deps (build-deps/deps-edn app-dir)))

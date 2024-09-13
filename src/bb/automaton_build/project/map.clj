(ns automaton-build.project.map
  "The project map gathers all useful informations for a project"
  (:require
   [automaton-build.os.file                 :as build-file]
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.project.config          :as build-project-config]
   [automaton-build.project.deps            :as build-deps]
   [automaton-build.project.package-json    :as project-package-json]
   [automaton-build.project.shadow          :as build-project-shadow]
   [automaton-build.project.tailwind-config :as project-tailwind-config]))

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
           :project-config-filedesc (when-not (:invalid? project-config-filedesc)
                                      project-config-filedesc)
           :app-name (get-in project-config-filedesc [:edn :app-name]))))

(defn add-deps-edn
  "Adds `:deps` key to to project map with the `deps.edn` file descriptor."
  [{:keys [app-dir]
    :as project-map}]
  (assoc project-map :deps (build-deps/deps-edn app-dir)))

(defn add-shadow-cljs
  "Adds `:shadow-cljs` key to to project map with the `shadow-cljs.edn` file descriptor."
  [{:keys [app-dir]
    :as project-map}]
  (assoc project-map
         :shadow-cljs
         (-> app-dir
             build-project-shadow/filename
             build-project-shadow/read)))

(defn add-tailwind-config
  "Adds `:tailwind-config` key to to project map with the `tailwind.config.js` file descriptor."
  [{:keys [app-dir]
    :as project-map}]
  (assoc project-map :tailwind-config (project-tailwind-config/load-tailwind-config app-dir)))

(defn add-package-json
  "Adds `:package-json` key to to project map with the `tailwind.config.js` file descriptor."
  [{:keys [app-dir]
    :as project-map}]
  (assoc project-map :package-json (project-package-json/load-package-json app-dir)))

(defn add-custom-css
  "Adds `:custom-css` key to to project map"
  [{:keys [app-dir]
    :as project-map}]
  (let [css (->> (get-in project-map [:project-config-filedesc :edn :frontend :css])
                 (build-filename/create-file-path app-dir)
                 build-file/read-file)]
    (assoc project-map :custom-css css)))

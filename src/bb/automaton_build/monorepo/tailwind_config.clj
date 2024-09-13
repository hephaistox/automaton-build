(ns automaton-build.monorepo.tailwind-config
  (:require
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.os.js-file              :as build-js-file]
   [automaton-build.project.tailwind-config :as project-tailwind-config]
   [clojure.string                          :as str]))

(defn- tailwind-content
  "Returns string with `app` tailwind content"
  [app]
  (let [app-dir (build-filename/create-dir-path "./" (:app-dir app))]
    (project-tailwind-config/dir->tailwind-content-path app-dir)))

(defn generate-tailwind-config
  "Creates the composite monorepo tailwind.config.js file"
  [main-tailwind-config apps]
  (let [tailwind-paths (->> apps
                            (map tailwind-content)
                            build-js-file/join-config-items)]
    (str/replace main-tailwind-config
                 #"content:\s*\[(.*?)\]"
                 (str "content: [" tailwind-paths "]"))))

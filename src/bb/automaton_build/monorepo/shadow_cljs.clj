(ns automaton-build.monorepo.shadow-cljs
  (:require
   [automaton-build.data.map :as build-data-map]))

(defn app-build-config-aliases
  [app]
  (let [build-aliases (get-in app [:project-config-filedesc :edn :frontend :run-aliases])
        shadow-cljs-builds (select-keys (get-in app [:shadow-cljs :edn :builds]) build-aliases)]
    {:builds shadow-cljs-builds}))

(defn generate-shadow-cljs
  "Creates the composite shadow-cljs file"
  [main-shadow-cljs apps]
  (->> apps
       (map app-build-config-aliases)
       (apply build-data-map/deep-merge main-shadow-cljs)))

(ns automaton-build.monorepo.tasks.clean
  "Clean monorepo temporary files"
  (:require
   [automaton-build.adapters.files :as files]
   [automaton-build.env-setup :as env-setup]))

(def env-setup env-setup/env-setup)

(defn clean
  "Clean small files to be deleted without consequences"
  [_apps _task-params]
  (->> "**{.DS_Store,.nrepl-port,.cpcache,/.cache,.log,.clj-kondo/.cache,/target}"
       (files/search-files ".")
       files/delete-files)
  (files/delete-files [(get-in env-setup [:log :spitted-edns])
                       (get-in env-setup [:published-apps :dir])
                       (get-in env-setup [:tests :tmp-dirs])]))

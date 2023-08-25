(ns automaton-build.app.running
  "Function to run the code of one application"
  (:require
   [automaton-build.adapters.commands :as cmds]
   [automaton-build.adapters.mermaid :as mermaid]
   [automaton-build.adapters.frontend-compiler :as frontend-compiler]
   [automaton-build.env-setup :as env-setup]))

(defn run-prepl
  "Execute the repl for the application which name is the first parameter
  Params:
  * `app` is the name of the app to launch
  * `repl-alias` is the name of the alias to run for repl"
  [{:keys [app-name] :as _app} repl-alias]
  (future
    (mermaid/watch (get-in env-setup/env-setup [:archi :dir])))
  (cmds/exec-cmds [[["clojure" (str "-M:" repl-alias) app-name]
                    ;; Leave :out in std. Otherwise, adding :out string is not compatible with blocking?
                    {:blocking? false
                     :dir "."}]]))

(defn run-prod-be
  "Execute the backend in production mode for the application which name is the first parameter"
  [{:keys [app-dir] :as  _app}]
  (cmds/exec-cmds [[["clojure" "-M:run"]]]
                  {:dir app-dir
                   :out :string}))

(defn watch-cljs
  "Compile and watch the cljs code to produce the frontend
  That watch is skipped if `frontend?` is false
  Params:
  * `app` application"
  [{:keys [app-dir frontend?] :as _app}]
  (when frontend?
    (frontend-compiler/watch-modifications app-dir)))

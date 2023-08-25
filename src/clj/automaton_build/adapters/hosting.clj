(ns automaton-build.adapters.hosting
  "Manage hosting on clever cloud
  Proxy to clever cli tool"
  (:require
   [automaton-build.adapters.commands :as cmds]))

(defn hosting-installed?*
  "Check clever cloud is useable
  Params:
  * `cc-command` (Optional, default clever) name of the command for clever cloud"
  ([cc-command]
   (try
     (cmds/exec-cmds [[[cc-command "version"]]]
                     {:out :string
                      :dir "."})
     (catch Exception e
       (throw (ex-info "Clever cloud is not installed properly"
                       {:exception e})))))
  ([]
   (hosting-installed?* "clever")))

(def hosting-installed?
  "Check clever cloud is useable"
  hosting-installed?*)

(defn prod-ssh
  "Connect to the production server
  Params:
  * `dir` the root directory where `clever` cli json is stored"
  [dir]
  (hosting-installed?)
  (cmds/exec-cmds [[["clever" "ssh"]]]
                  {:dir dir
                   :out :string}))

(defn upsert-cc-app
  "Not implemented yet
  Params:
  * `app-name` application name in clever cloud
  * `dir` directory where to execute"
  [app-name dir]
  (hosting-installed?)
  (cmds/exec-cmds [[["clever" "create" "--type" "docker" "--org" "Hephaistox" "--region" "par" app-name]]]
                  {:out :string
                   :dir dir}))

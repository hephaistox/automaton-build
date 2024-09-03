(ns automaton-build.os.npm
  "Proxy to npm features"
  (:require
   [automaton-build.os.commands :as build-cmds]))

(defn npm-install-cmd "Command to install the current modules" [dir] ["npm" "install" {:dir dir}])

(defn npm-audit-fix-cmd "Audit existing npm packages" [] ["npm" "audit" "fix"])

(defn npm-update-cmd "Command to update the dependencies" [] ["npm" "update"])

(defn- npx-installed?*
  "Check if npx is installed

  Params:
  * `dir` where npx should be executed
  * `npx-cmd` (Optional, default=npx) parameter to tell the npx command"
  ([dir] (npx-installed?* dir "npx"))
  ([dir npx-cmd]
   (every? zero? (mapv first (build-cmds/execute-with-exit-code [npx-cmd "-v" {:dir dir}])))))

(def npx-installed? (memoize npx-installed?*))

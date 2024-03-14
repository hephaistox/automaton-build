(ns automaton-build.code-helpers.update-deps
  (:require
   [automaton-build.app-data :as build-app-data]
   [automaton-build.code-helpers.antq :as build-code-helpers-antq]
   [automaton-build.log :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.npm :as build-npm]))

(defn update-app-deps
  "Update all deps.edn dependencies in `app-dir` excluding `exclude-libs`"
  [app-dir exclude-libs exclude-projects]
  (let [dirs-to-update (build-app-data/project-root-dirs app-dir
                                                         exclude-projects)]
    (build-log/info "Updating npm libraries...")
    (if (every? true?
                (map #(do (build-log/debug-format "Updating npm of %s" %)
                          (zero? (ffirst (build-cmds/execute-with-exit-code
                                          (build-npm/npm-install-cmd %)
                                          (build-npm/npm-update-cmd %)))))
                     dirs-to-update))
      (do (apply build-code-helpers-antq/do-update exclude-libs dirs-to-update)
          true)
      (do (build-log/warn "Npm projects version update failed") false))))

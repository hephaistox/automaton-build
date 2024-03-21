(ns automaton-build.code-helpers.update-deps
  (:require
   [automaton-build.code-helpers.antq :as build-code-helpers-antq]
   [automaton-build.log               :as build-log]
   [automaton-build.os.commands       :as build-cmds]
   [automaton-build.os.npm            :as build-npm]))

(defn update-frontend-deps
  [app-dir]
  (zero? (ffirst (build-cmds/execute-with-exit-code
                  (build-npm/npm-install-cmd app-dir)
                  (build-npm/npm-update-cmd app-dir)
                  (build-npm/npm-audit-fix-cmd app-dir)))))

(defn update-app-deps
  "Update all deps.edn dependencies in `app-dir` excluding `exclude-libs`"
  [app-dir exclude-libs]
  (build-log/info "Updating npm libraries...")
  (if (update-frontend-deps app-dir)
    (do (build-log/info "Updating deps.edn file")
        (build-code-helpers-antq/do-update exclude-libs app-dir)
        true)
    (do (build-log/warn "Npm projects version update failed") false)))

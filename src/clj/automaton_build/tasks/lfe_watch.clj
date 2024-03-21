(ns automaton-build.tasks.lfe-watch
  (:require
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.log                            :as build-log]
   [automaton-build.os.exit-codes                  :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Compile local modifications for development environment and watch the modifications"
  [_task-map
   {:keys [app-dir publication]
    :as _app-data}]
  (let [frontend (:frontend publication)]
    (if (not (build-frontend-compiler/is-frontend-project? app-dir))
      (do (build-log/fatal "Is not a frontend project")
          build-exit-codes/catch-all)
      (if (empty? frontend)
        (do
          (build-log/warn
           "Skip the frontend watch as no setup is found in build_config.edn for key `[:publication :frontend]`")
          build-exit-codes/cannot-execute)
        (let [{:keys [run-aliases]} frontend
              run-aliases-strs (mapv name run-aliases)]
          (if-not (build-frontend-compiler/fe-watch app-dir run-aliases-strs)
            (do (build-log/fatal "Tests have failed")
                build-exit-codes/catch-all)
            build-exit-codes/ok))))))

(comment
  (build-frontend-compiler/fe-watch "" ["app" "ltest"]))

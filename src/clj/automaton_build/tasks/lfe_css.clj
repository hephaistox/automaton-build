(ns automaton-build.tasks.lfe-css
  (:require
   [automaton-build.app.files-css             :as build-app-files-css]
   [automaton-build.code-helpers.frontend-css :as build-frontend-css]
   [automaton-build.log                       :as build-log]
   [automaton-build.os.exit-codes             :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Compile local modifications for development environment and watch the modifications"
  [_task-map
   {:keys [app-dir publication]
    :as _app-data}]
  (let [frontend (:frontend publication)]
    (if (empty? frontend)
      (do
        (build-log/warn
         "Skip the frontend watch as no setup is found in build_config.edn for key `[:publication :frontend]`")
        build-exit-codes/cannot-execute)
      (try (let [{:keys [css compiled-styles-css]} frontend
                 {:keys [main-css custom-css]} css
                 combined-css-file (apply build-app-files-css/combine-css-files
                                          [main-css custom-css])]
             (build-log/info "Start watching css")
             (if-not (build-frontend-css/fe-css-watch app-dir combined-css-file compiled-styles-css)
               (do (build-log/fatal "Css watch has failed") build-exit-codes/catch-all)
               build-exit-codes/ok))
           (catch Exception e
             (build-log/error "Impossible to execute")
             (build-log/error-exception e)
             build-exit-codes/catch-all)))))

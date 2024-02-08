(ns automaton-build.tasks.lfe-watch
  (:require
   [automaton-build.app.files-css :as build-app-files-css]
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Compile local modifications for development environment and watch the modifications"
  [_task-map
   {:keys [app-dir publication]
    :as _app-data}]
  (let [frontend (:frontend publication)]
    (when (build-frontend-compiler/is-shadow-project? app-dir)
      (if (empty? frontend)
        (do
          (build-log/warn
           "Skip the frontend watch as no setup is found in build_config.edn for key `[:publication :frontend]`")
          build-exit-codes/cannot-execute)
        (let [{:keys [css compiled-styles-css run-aliases]} frontend
              {:keys [main-css custom-css]} css
              combined-css-file (apply build-app-files-css/combine-css-files
                                       [main-css custom-css])
              run-aliases-strs (mapv name run-aliases)]
          (if-not (build-frontend-compiler/fe-watch app-dir
                                                    run-aliases-strs
                                                    combined-css-file
                                                    compiled-styles-css)
            (do (build-log/fatal "Tests have failed")
                build-exit-codes/catch-all)
            build-exit-codes/ok))))))

(comment
  (build-frontend-compiler/fe-watch ""
                                    ["app" "karma-test"]
                                    (apply build-app-files-css/combine-css-files
                                           ["resources/css/main.css"
                                            "resources/css/customer.css"])
                                    "target/foo.css"))

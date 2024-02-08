(ns automaton-build.tasks.build-jar
  (:require
   [automaton-build.app.compile :as build-app-compile]
   [automaton-build.configuration :as build-conf]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files :as build-files]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [app-name app-dir deps-edn publication]}]
  (let [{:keys [as-lib
                class-dir
                target-jar-filename
                jar-main
                deploy-to
                frontend
                license
                env]}
        publication
        {:keys [deploy-alias css compiled-styles-css]} frontend
        {:keys [main-css custom-css]} css
        target-env (build-conf/read-param [:env])
        jar-path (->> (format target-jar-filename (name target-env) app-name)
                      (build-files/create-file-path app-dir)
                      build-files/absolutize)
        exclude-aliases (get-in env [target-env :exclude-aliases])]
    (if (build-app-compile/compile target-env
                                   app-dir
                                   deploy-to
                                   deps-edn
                                   exclude-aliases
                                   as-lib
                                   class-dir
                                   jar-path
                                   jar-main
                                   deploy-alias
                                   main-css
                                   custom-css
                                   compiled-styles-css
                                   license)
      build-exit-codes/ok
      build-exit-codes/catch-all)))

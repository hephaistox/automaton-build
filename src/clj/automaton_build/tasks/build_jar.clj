(ns automaton-build.tasks.build-jar
  (:require
   [automaton-build.app.compile   :as build-app-compile]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files      :as build-files]
   [automaton-build.utils.keyword :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [app-name app-dir deps-edn publication environment]}]
  (let [environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        {:keys [class-dir target-jar-filename jar-main deploy-to frontend env]}
        publication
        {:keys [deploy-alias css compiled-styles-css]} frontend
        {:keys [main-css custom-css]} css
        jar-path (->> (format target-jar-filename (name environment) app-name)
                      (build-files/create-file-path app-dir)
                      build-files/absolutize)
        exclude-aliases (get-in env [environment :exclude-aliases])]
    (if (build-app-compile/compile environment
                                   app-dir
                                   deploy-to
                                   deps-edn
                                   exclude-aliases
                                   class-dir
                                   jar-path
                                   jar-main
                                   deploy-alias
                                   main-css
                                   custom-css
                                   compiled-styles-css)
      build-exit-codes/ok
      build-exit-codes/catch-all)))

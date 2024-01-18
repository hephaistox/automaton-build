(ns automaton-build.tasks.build-jar
  (:require
   [automaton-build.app.compile :as build-app-compile]
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
        {:keys [deploy-alias main-css custom-css compiled-styles-css]} frontend
        jar-path (->> (format target-jar-filename (name :production) app-name)
                      (build-files/create-file-path app-dir)
                      build-files/absolutize)
        exclude-aliases (get-in env [:production :exclude-aliases])]
    (if (build-app-compile/compile :production
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

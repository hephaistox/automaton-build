(ns automaton-build.tasks.publish-library
  (:require
   [automaton-build.cicd.deployment :as build-deployment]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files :as build-files]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Publish project as a library (jar) to clojars."
  [_task-map
   {:keys [publication app-dir app-name]
    :as _app-data}]
  (let [{:keys [target-jar-filename]} publication
        jar-path (->> (format target-jar-filename (name :production) app-name)
                      (build-files/create-file-path app-dir)
                      build-files/absolutize)]
    (if (build-deployment/publish-library
         jar-path
         (build-files/create-file-path app-dir "pom.xml"))
      build-exit-codes/ok
      build-exit-codes/catch-all)))

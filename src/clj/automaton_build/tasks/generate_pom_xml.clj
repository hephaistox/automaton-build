(ns automaton-build.tasks.generate-pom-xml
  (:require
   [automaton-build.app.deps-edn            :as build-deps-edn]
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]
   [automaton-build.log                     :as build-log]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.utils.keyword           :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Publish project as a runnable app (uber-jar) to clever cloud."
  [_task-map
   {:keys [app-name app-dir deps-edn publication environment]
    :as _app-data}]
  (let [environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        {:keys [as-lib deploy-to license env]} publication
        exclude-aliases (get-in env [environment :exclude-aliases])
        paths (build-deps-edn/extract-paths deps-edn exclude-aliases)
        app-source-paths (->> paths
                              (filter #(re-find #"src" %))
                              (filter #(not (re-find #"development" %))))]
    (build-log/info-format "Geneartion of pom-xml started for `%s`" app-name)
    (if (= deploy-to :clojars)
      (if (nil? (build-pom-xml/generate-pom-xml as-lib
                                                app-source-paths
                                                app-dir
                                                license))
        build-exit-codes/ok
        build-exit-codes/catch-all)
      (do (build-log/debug
           "Generation of pom.xml skipped as it's used only for libraries")
          build-exit-codes/ok))))

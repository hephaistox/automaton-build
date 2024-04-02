(ns automaton-build.tasks.generate-pom-xml
  (:require
   [automaton-build.app.deps-edn            :as build-deps-edn]
   [automaton-build.cicd.cfg-mgt            :as build-cfg-mgt]
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]
   [automaton-build.cicd.version            :as build-version]
   [automaton-build.log                     :as build-log]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.utils.keyword           :as build-utils-keyword]))

(defn generate-pom-xml?
  "POM xml should be generated only if version file has changed - for optimization"
  [app-dir]
  (build-cfg-mgt/file-modified? (build-version/version-file app-dir)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Generate pom.xml file"
  [_task-map
   {:keys [app-name app-dir deps-edn publication environment force]
    :as _app-data}]
  (if (or force (generate-pom-xml? app-dir))
    (let [environment (-> environment
                          build-utils-keyword/trim-colon
                          build-utils-keyword/keywordize)
          {:keys [as-lib deploy-to license env]} publication
          exclude-aliases (get-in env [environment :exclude-aliases])
          paths (build-deps-edn/extract-paths deps-edn exclude-aliases)
          app-source-paths (->> paths
                                (filter #(re-find #"src" %))
                                (filter #(not (re-find #"development" %))))]
      (build-log/info-format "Generation of pom-xml started for `%s`" app-name)
      (if (= deploy-to :clojars)
        (if (nil? (build-pom-xml/generate-pom-xml as-lib
                                                  app-source-paths
                                                  app-dir
                                                  license))
          build-exit-codes/ok
          build-exit-codes/catch-all)
        (do (build-log/debug
             "Generation of pom.xml skipped as it's used only for libraries")
            build-exit-codes/ok)))
    build-exit-codes/ok))

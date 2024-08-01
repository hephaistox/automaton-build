(ns automaton-build.tasks.generate-pom-xml
  (:require
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]
   [automaton-build.log                     :as build-log]
   [automaton-build.os.exit-codes           :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Generate pom.xml file"
  [_task-map
   {:keys [app-name app-dir publication]
    :as _app-data}]
  (let [{:keys [as-lib deploy-to license]} publication]
    (build-log/info-format "Generation of pom-xml started for `%s`" app-name)
    (if (= deploy-to :clojars)
      (if (nil? (build-pom-xml/generate-pom-xml as-lib ["src"] app-dir license))
        build-exit-codes/ok
        build-exit-codes/catch-all)
      (do (build-log/debug
           "Generation of pom.xml skipped as it's used only for libraries")
          build-exit-codes/ok))))

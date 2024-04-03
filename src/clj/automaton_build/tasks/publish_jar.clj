(ns automaton-build.tasks.publish-jar
  (:require
   [automaton-build.cicd.deployment         :as build-deployment]
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]
   [automaton-build.cicd.version            :as build-version]
   [automaton-build.log                     :as build-log]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.os.files                :as build-files]
   [automaton-build.utils.keyword           :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Publish project jar to target platform"
  [_task-map
   {:keys [app-name publication environment app-dir]
    :as _app-data}]
  (let [{:keys [deploy-to target-jar-filename env]} publication
        environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        jar-path (->> (format target-jar-filename (name environment) app-name)
                      (build-files/create-file-path app-dir)
                      build-files/absolutize)
        clever-uri (get-in env [environment :clever-uri])]
    (build-log/info-format "Deployment process started to `%s`" deploy-to)
    (if (case deploy-to
          :clojars (build-deployment/publish-library jar-path
                                                     (build-pom-xml/pom-xml
                                                      app-dir))
          :cc (build-deployment/publish-app
               clever-uri
               (->> (name environment)
                    (build-files/create-dir-path app-dir "target")
                    build-files/absolutize)
               (->> ".clever"
                    (build-files/create-dir-path app-dir)
                    build-files/absolutize)
               (build-version/current-version app-dir))
          (do (build-log/info-format
               "Deploy skipped as deploy-to param is missing. It's `%s`"
               deploy-to)
              true))
      build-exit-codes/ok
      build-exit-codes/catch-all)))

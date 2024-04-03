(ns automaton-build.cicd.deployment
  (:require
   [automaton-build.cicd.clever-cloud   :as build-clever-cloud]
   [automaton-build.cicd.deployment.jar :as build-deploy-jar]
   [automaton-build.configuration       :as build-conf]
   [automaton-build.os.files            :as build-files]))

(defn publish-library
  "Publish jar to clojars"
  [jar-path pom-path]
  (build-deploy-jar/deploy
   jar-path
   pom-path
   {"clojars" {:url "https://clojars.org/repo"
               :username (build-conf/read-param [:clojars-username])
               :password (build-conf/read-param [:clojars-password])}})
  true)

(defn publish-app
  "Publish uber-jar to Clever Cloud. [clever docs](https://developers.clever-cloud.com/doc/cli/)"
  ([repo-uri target-dir clever-dir version]
   (let [clever-repo-dir
         (build-clever-cloud/clone-repo clever-dir repo-uri "repo")]
     (build-files/copy-files-or-dir [target-dir]
                                    (build-files/create-dir-path clever-repo-dir
                                                                 "target"))
     (build-clever-cloud/deploy clever-repo-dir version)))
  ([repo-uri target-dir]
   (publish-app repo-uri
                target-dir
                (->> ".clever"
                     (build-files/create-dir-path ".")
                     build-files/absolutize)
                "Automatically pushed version")))

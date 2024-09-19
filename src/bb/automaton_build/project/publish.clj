(ns automaton-build.project.publish
  (:require
   [automaton-build.echo.headers                     :refer [normalln]]
   [automaton-build.os.cmds                          :as build-commands]
   [automaton-build.os.filename                      :as build-filename]
   [automaton-build.project.impl.clever-cloud-deploy :as build-clever-cloud]
   [automaton-build.project.impl.clojars-deploy      :as build-deploy-jar]
   [automaton-build.tasks.impl.headers.files         :as build-headers-files]))

(defn publish-clojars
  "Publish jar to clojars"
  [jar-path app-dir]
  (normalln (build-commands/blocking-cmd (build-deploy-jar/deploy jar-path) app-dir))
  #_(build-deploy-jar/deploy jar-path
                             pom-path
                             {"clojars"
                              {:url "https://clojars.org/repo"
                               :username (build-project-conf/read-param [:clojars-username])
                               :password (build-project-conf/read-param [:clojars-password])}}))

(defn publish-clever-cloud
  "Publish uber-jar to Clever Cloud. [clever docs](https://developers.clever-cloud.com/doc/cli/)"
  ([repo-uri target-dir clever-dir version]
   (let [clever-repo-dir (build-clever-cloud/clone-repo clever-dir repo-uri "repo")]
     (build-headers-files/copy-files target-dir
                                     (build-filename/create-dir-path clever-repo-dir "target")
                                     "*"
                                     false
                                     {})
     (build-clever-cloud/deploy clever-repo-dir version)))
  ([repo-uri target-dir]
   (publish-clever-cloud repo-uri
                         target-dir
                         (->> ".clever"
                              (build-filename/create-dir-path ".")
                              build-filename/absolutize)
                         "Automatically pushed version")))

(ns automaton-build.project.publish
  (:require
   [automaton-build.os.cmds                          :as build-commands]
   [automaton-build.os.filename                      :as build-filename]
   [automaton-build.project.configuration            :as build-project-conf]
   [automaton-build.project.impl.clever-cloud-deploy :as build-clever-cloud]
   [automaton-build.project.impl.clojars-deploy      :as build-deploy-jar]
   [automaton-build.project.pom-xml                  :as build-project-pom-xml]
   [automaton-build.tasks.impl.headers.files         :as build-headers-files]))

(defn generate-pom-xml
  [app-dir as-lib license source-paths]
  (if-let [pom-res (build-project-pom-xml/generate-pom-xml as-lib source-paths app-dir license)]
    {:status :failed
     :res pom-res}
    {:status :success}))

(defn pom-xml-status
  [app-dir as-lib pom-xml-license paths]
  (if (and app-dir as-lib pom-xml-license)
    (generate-pom-xml app-dir as-lib pom-xml-license paths)
    {:status :failed
     :app-dir app-dir
     :as-lib as-lib
     :pom-xml-license pom-xml-license
     :msg "Missing required-params"}))

(defn publish-clojars
  "Publish jar to clojars"
  [jar-path app-dir paths as-lib pom-xml-license]
  (if (and (build-project-conf/read-param [:clojars-username])
           (build-project-conf/read-param [:clojars-password]))
    (let [pom-xml-status (pom-xml-status app-dir as-lib pom-xml-license paths)]
      (if (= :success (:status pom-xml-status))
        (let [deploy-res (build-commands/blocking-cmd (build-deploy-jar/deploy jar-path) app-dir)]
          (if (build-commands/success deploy-res)
            {:status :success}
            {:status :failed
             :res deploy-res}))
        {:status :failed
         :message "Pom xml generation failed"
         :res pom-xml-status}))
    {:status :skipped
     :message
     "missing params, make sure you've run ENV CLOJARS_USERNAME=username CLOJARS_PASSWORD=password"}))

(defn publish-clever-cloud
  "Publish uber-jar to Clever Cloud. [clever docs](https://developers.clever-cloud.com/doc/cli/)"
  ([repo-uri target-dir clever-dir version]
   (let [clever-repo-dir (build-clever-cloud/clone-repo clever-dir repo-uri "repo")]
     (build-headers-files/copy-files target-dir
                                     (build-filename/create-dir-path clever-repo-dir "target")
                                     "*"
                                     false
                                     {})
     (let [res (build-clever-cloud/deploy clever-repo-dir version)]
       (if (build-commands/success res)
         {:stauts :success}
         {:status :failed
          :res res}))))
  ([repo-uri target-dir]
   (publish-clever-cloud repo-uri
                         target-dir
                         (->> ".clever"
                              (build-filename/create-dir-path ".")
                              build-filename/absolutize)
                         "Automatically pushed version")))

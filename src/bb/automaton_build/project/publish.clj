(ns automaton-build.project.publish
  (:require
   [automaton-build.echo.headers                     :refer [build-writter normalln]]
   [automaton-build.os.cmds                          :as build-commands]
   [automaton-build.os.file                          :as build-file]
   [automaton-build.os.filename                      :as build-filename]
   [automaton-build.project.configuration            :as build-project-conf]
   [automaton-build.project.impl.clever-cloud-deploy :as build-clever-cloud]
   [automaton-build.project.impl.clojars-deploy      :as build-deploy-jar]
   [automaton-build.tasks.impl.headers.files         :as build-headers-files]))

(defn publish-clojars
  "Publish jar to clojars"
  [jar-path app-dir]
  (if (and (build-project-conf/read-param [:clojars-username])
           (build-project-conf/read-param [:clojars-password]))
    (let [s (build-writter)
          deploy-res (binding [*out* s
                               *err* s]
                       (build-commands/blocking-cmd (build-deploy-jar/deploy-cmd jar-path)
                                                    app-dir))]
      (if (build-commands/success deploy-res)
        {:status :success
         :msg (str s)}
        {:status :failed
         :msg (str s)
         :res deploy-res}))
    {:status :failed
     :message
     "missing params, make sure you've run ENV CLOJARS_USERNAME=username CLOJARS_PASSWORD=password"}))

(defn publish-clever-cloud
  "Publish uber-jar to Clever Cloud. [clever docs](https://developers.clever-cloud.com/doc/cli/)"
  ([repo-uri target-dir clever-dir version verbose?]
   (let [clever-repo-res (build-clever-cloud/clone-repo clever-dir repo-uri "repo")
         clever-repo-dir (:filepath clever-repo-res)
         clever-target (build-filename/create-dir-path clever-repo-dir "target")]
     (if (= :success (:status clever-repo-res))
       (do (build-file/delete-path clever-target)
           (build-file/ensure-dir-exists clever-target)
           (build-headers-files/copy-files target-dir clever-target "*" verbose? {})
           (let [res (build-clever-cloud/deploy clever-repo-dir version)]
             (if (build-commands/success res)
               {:status :success}
               {:status :failed
                :res res})))
       {:status :failed
        :message "Cloning CC failed"
        :res (:res clever-repo-res)})))
  ([repo-uri target-dir verbose?]
   (publish-clever-cloud repo-uri
                         target-dir
                         (->> ".clever"
                              (build-filename/create-dir-path ".")
                              build-filename/absolutize)
                         "Automatically pushed version"
                         verbose?)))

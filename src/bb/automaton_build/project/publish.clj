(ns automaton-build.project.publish
  (:require
   [automaton-build.echo.headers                     :refer [build-writter normalln]]
   [automaton-build.os.cmds                          :as build-commands]
   [automaton-build.os.file                          :as build-file]
   [automaton-build.os.filename                      :as build-filename]
   [automaton-build.project.configuration            :as build-project-conf]
   [automaton-build.project.impl.clever-cloud-deploy :as build-clever-cloud]
   [automaton-build.project.impl.clojars-deploy      :as build-deploy-jar]
   [automaton-build.project.pom-xml                  :as build-project-pom-xml]
   [automaton-build.tasks.impl.headers.files         :as build-headers-files]
   [clojure.string                                   :as str]))

(defn generate-pom-xml
  [app-dir as-lib license source-paths]
  (let [s# (new java.io.StringWriter)
        ;; _ (require 'clojure.tools.deps.util.io :reload)
        res (binding [*out* s#
                      *err* s#]
              (build-project-pom-xml/generate-pom-xml as-lib source-paths app-dir license))]
    res))

(comment
  (require 'clojure.tools.deps.util.io :reload)
  (build-project-pom-xml/generate-pom-xml 'org.clojars.hephaistox/automaton-core
                                          ["src" "src/clj" "src/cljs"]
                                          "automaton/automaton_core"
                                          {:name "CC BY-NC 4.0"
                                           :url
                                           "https://cretivecommons.org/licenses/by-nc/4.0/deed.en"})
  (generate-pom-xml "automaton/automaton_core"
                    'org.clojars.hephaistox/automaton-core
                    {:name "CC BY-NC 4.0"
                     :url "https://creativecommons.org/licenses/by-nc/4.0/deed.en"}
                    ["dupa/src/cljc" "dontexist/src/clj" "whatever/src/cljs"])
  ;
)

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
  [jar-path app-dir paths as-lib pom-xml-license verbose?]
  (if (and (build-project-conf/read-param [:clojars-username])
           (build-project-conf/read-param [:clojars-password]))
    (let [_ (normalln "pom-xml generation")
          pom-xml-status (pom-xml-status app-dir as-lib pom-xml-license paths)]
      (when (and verbose? (:msg pom-xml-status) (not (str/blank? (:msg pom-xml-status))))
        (normalln (:msg pom-xml-status)))
      (if (= :success (:status pom-xml-status))
        (let [_ (normalln "deploy itself")
              s (build-writter)
              deploy-res (binding [*out* s
                                   *err* s]
                           (build-commands/blocking-cmd (build-deploy-jar/deploy-cmd jar-path)
                                                        app-dir))]
          (if (build-commands/success deploy-res)
            {:status :success
             :msg (str s)}
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

(ns automaton-build.cicd.deployment.pom-xml
  "Code related to pom.xml file that is used for deployment"
  (:require
   [automaton-build.cicd.version           :as build-version]
   [automaton-build.code-helpers.artifacts :as build-helpers-artifacts]
   [automaton-build.log                    :as build-log]
   [automaton-build.os.files               :as build-files]))

(defn pom-xml
  "pom.xml file name and location"
  ([] "pom.xml")
  ([project-root] (build-files/absolutize (build-files/create-file-path project-root (pom-xml)))))

(defn generate-pom-xml
  "Generates pom.xml in the root of the project"
  [as-lib app-source-paths project-root license]
  (build-helpers-artifacts/set-project-root! (build-files/absolutize project-root))
  (let [pom-data [[:licenses [:license [:name (:name license)] [:url (:url license)]]]]
        basis (build-helpers-artifacts/create-basis)
        version (build-version/current-version project-root)]
    (build-log/debug "Write POM files")
    (build-helpers-artifacts/write-pom (merge {:target (build-files/absolutize project-root)
                                               :lib as-lib
                                               :version version
                                               :basis basis
                                               :src-dirs app-source-paths}
                                              (when pom-data {:pom-data pom-data})))))

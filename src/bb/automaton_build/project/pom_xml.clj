(ns automaton-build.project.pom-xml
  (:require
   [automaton-build.code.artifacts :as build-code-artifacts]
   [automaton-build.echo.headers   :refer [build-writter]]
   [automaton-build.os.filename    :as build-filename]
   [automaton-build.os.version     :as build-version]))

(defn pom-xml
  "pom.xml file name and location"
  ([] "pom.xml")
  ([project-root]
   (build-filename/absolutize (build-filename/create-file-path project-root (pom-xml)))))

(defn generate-pom-xml
  "Generates pom.xml in the root of the project"
  ([as-lib app-source-paths project-root license]
   (generate-pom-xml as-lib
                     app-source-paths
                     project-root
                     license
                     (build-version/current-version project-root)))
  ([as-lib app-source-paths project-root license version]
   (try (let [s (build-writter)
              pom-data [[:licenses [:license [:name (:name license)] [:url (:url license)]]]]
              _set-root! (binding [*out* s
                                   *err* s]
                           (build-code-artifacts/set-project-root! (build-filename/absolutize
                                                                    project-root)))
              basis (binding [*out* s *err* s] (build-code-artifacts/create-basis))
              pom-res (binding [*out* s
                                *err* s]
                        (build-code-artifacts/write-pom
                         (merge {:target (build-filename/absolutize project-root)
                                 :lib as-lib
                                 :version version
                                 :basis basis
                                 :src-dirs app-source-paths}
                                (when pom-data {:pom-data pom-data}))))]
          {:status :success
           :res pom-res
           :msg (str s)})
        (catch Exception e
          {:status :failed
           :ex e}))))

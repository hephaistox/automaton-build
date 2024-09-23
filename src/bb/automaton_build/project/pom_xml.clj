(ns automaton-build.project.pom-xml
  (:require
   [automaton-build.code.artifacts :as build-code-artifacts]
   [automaton-build.os.filename    :as build-filename]
   [automaton-build.os.version     :as build-version]))

(defn pom-xml
  "pom.xml file name and location"
  ([] "pom.xml")
  ([project-root]
   (build-filename/absolutize (build-filename/create-file-path project-root (pom-xml)))))

(defn generate-pom-xml
  "Generates pom.xml in the root of the project."
  ([as-lib app-source-paths project-root license]
   (generate-pom-xml as-lib
                     app-source-paths
                     project-root
                     license
                     (build-version/current-version project-root)))
  ([as-lib app-source-paths project-root license version]
   (try (let [pom-data [[:licenses [:license [:name (:name license)] [:url (:url license)]]]]
              s# (new java.io.StringWriter)
              ;; This require is needed to go around this issue: https://github.com/babashka/pods/issues/72
              _ (require 'clojure.tools.deps.util.io :reload)
              _set-root! (binding [*err* s#]
                           (build-code-artifacts/set-project-root! (build-filename/absolutize
                                                                    project-root)))
              basis (binding [*err* s#] (build-code-artifacts/create-basis))
              pom-res (binding [*err* s#]
                        (build-code-artifacts/write-pom
                         (merge {:target (build-filename/absolutize project-root)
                                 :lib as-lib
                                 :version version
                                 :basis basis
                                 :src-dirs app-source-paths}
                                (when pom-data {:pom-data pom-data}))))]
          {:status :success
           :msg (str s#)
           :res pom-res})
        (catch Exception e
          {:status :failed
           :ex e}))))

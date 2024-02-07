(ns automaton-build.code-helpers.compiler.jar
  (:require
   [automaton-build.code-helpers.artifacts :as build-helpers-artifacts]
   [automaton-build.os.files :as build-files]
   [automaton-build.log :as build-log]))

(defn compile-jar
  "Creates pom.xml and jar from `class-dir`. Jar will be created in `jar-file` and `pom.xml` in the root of project directory."
  [class-dir jar-file project-root as-lib version pom-data app-source-paths]
  (build-helpers-artifacts/set-project-root! (build-files/absolutize
                                              project-root))
  (let [basis (build-helpers-artifacts/create-basis)]
    (build-log/debug "Write POM files")
    (build-helpers-artifacts/write-pom (merge {:class-dir class-dir
                                               :lib as-lib
                                               :version version
                                               :basis basis
                                               :src-dirs app-source-paths}
                                              (when pom-data
                                                {:pom-data pom-data})))
    (build-files/copy-files-or-dir
     [(build-files/create-file-path
       (build-files/create-dir-path class-dir "META-INF/maven" as-lib)
       "pom.xml")]
     (build-files/absolutize project-root))
    (build-helpers-artifacts/jar {:class-dir class-dir
                                  :jar-file jar-file})))

(defn compile-uber-jar
  "Compiles code from `class-dir` to `jar-file` that will be runnable by `jar-main`"
  [class-dir jar-file jar-main project-root]
  (build-helpers-artifacts/set-project-root! (build-files/absolutize
                                              project-root))
  (let [basis (build-helpers-artifacts/create-basis)]
    (build-helpers-artifacts/compile-clj
     {:basis basis
      :class-dir class-dir
      :java-opts
      ["-Dheph-conf=env/production/resources/config.edn,env/common_config.edn"]})
    (build-helpers-artifacts/uber {:class-dir class-dir
                                   :uber-file jar-file
                                   :basis basis
                                   :main jar-main})))

(ns automaton-build.code-helpers.compiler.jar
  (:require
   [automaton-build.code-helpers.artifacts :as build-helpers-artifacts]
   [automaton-build.os.files               :as build-files]))

(defn compile-jar
  "Creates new jar from `class-dir`. Jar will be created in `jar-file`."
  [class-dir jar-file project-root]
  (build-helpers-artifacts/set-project-root! (build-files/absolutize project-root))
  (build-helpers-artifacts/jar {:class-dir class-dir
                                :jar-file jar-file}))

(defn compile-uber-jar
  "Compiles code from `class-dir` to `jar-file` that will be runnable by `jar-main`"
  [class-dir jar-file jar-main project-root]
  (build-helpers-artifacts/set-project-root! (build-files/absolutize project-root))
  (let [basis (build-helpers-artifacts/create-basis)]
    (build-helpers-artifacts/compile-clj
     {:basis basis
      :class-dir class-dir
      :java-opts ["-Dheph-conf=env/production/resources/config.edn,env/common_config.edn"]})
    (build-helpers-artifacts/uber {:class-dir class-dir
                                   :uber-file jar-file
                                   :basis basis
                                   :main jar-main})))

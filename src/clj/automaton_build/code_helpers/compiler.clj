(ns automaton-build.code-helpers.compiler
  "Compiler of the project"
  (:require
   [automaton-build.app                       :as build-app]
   [automaton-build.code-helpers.compiler.jar :as build-compiler-jar]
   [automaton-build.log                       :as build-log]
   [automaton-build.os.files                  :as build-files]))

(defn compile-jar
  "Compile code to jar or uber-jar based on `jar-type`."
  [as-lib class-dir target-jar-path project-dir]
  (build-log/debug "Launch clj compilation")
  (try (build-log/trace-format "Jar is built `%s`" target-jar-path)
       (build-compiler-jar/compile-jar class-dir
                                       target-jar-path
                                       project-dir
                                       as-lib)
       (build-log/info-format "Compilation ending successfully: `%s`"
                              target-jar-path)
       target-jar-path
       (catch Exception e
         (build-log/error-exception (ex-info "Compilation failed"
                                             {:exception e}))
         nil)))

(defn compile-uber-jar
  "Compile code to jar or uber-jar based on `jar-type`."
  [class-dir target-jar-path paths project-dir jar-main]
  (let [app-paths (->> paths
                       (build-app/append-app-dir project-dir)
                       (map build-files/absolutize))]
    (build-log/debug "Launch clj compilation")
    (build-log/trace-format "Copy files from `%s` to `%s`" app-paths class-dir)
    (try (when (build-files/copy-files-or-dir app-paths class-dir)
           (build-log/trace-format "Jar is built `%s`" target-jar-path)
           (build-compiler-jar/compile-uber-jar class-dir
                                                target-jar-path
                                                jar-main
                                                project-dir)
           (build-log/info-format "Compilation ending successfully: `%s`"
                                  target-jar-path)
           target-jar-path)
         (catch Exception e
           (build-log/error-exception (ex-info "Compilation failed"
                                               {:exception e}))
           nil))))

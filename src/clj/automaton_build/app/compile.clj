(ns automaton-build.app.compile
  (:refer-clojure :exclude [compile])
  (:require
   [automaton-build.app :as build-app]
   [automaton-build.app.deps-edn :as build-deps-edn]
   [automaton-build.code-helpers.compiler :as build-compiler]
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]))

(defn prepare-compilation-files
  "Creates `app-dir` `paths` in a `target-dir`"
  [target-dir paths app-dir]
  (let [app-paths (->> paths
                       (build-app/append-app-dir app-dir)
                       (map build-files/absolutize))]
    (build-log/debug-format "Copy files from `%s` to `%s`" app-paths target-dir)
    (build-files/copy-files-or-dir app-paths target-dir)))

(defn compile-frontend
  [app-dir deploy-alias css-files compiled-css-path]
  (if (and (build-frontend-compiler/is-shadow-project? app-dir) deploy-alias)
    (do (build-frontend-compiler/compile-release deploy-alias
                                                 css-files
                                                 compiled-css-path
                                                 app-dir)
        (build-log/info "Frontend compiled"))
    (build-log/info "Frontend compilation skipped")))

(defn compile-backend
  [deploy-to app-dir paths as-lib class-dir jar-path jar-main env license]
  (let [env (name env)
        class-dir (build-files/absolutize (build-files/create-dir-path
                                           app-dir
                                           (format class-dir env)))]
    (prepare-compilation-files class-dir paths app-dir)
    (case deploy-to
      :clojars (build-compiler/compile-jar
                as-lib
                class-dir
                jar-path
                paths
                app-dir
                [[:licenses
                  [:license [:name (:name license)] [:url (:url license)]]]])
      :cc (build-compiler/compile-uber-jar class-dir
                                           jar-path
                                           paths
                                           app-dir
                                           jar-main))))

#_{:clj-kondo/ignore [:redefined-var]}
(defn compile
  "Compiles project backend and frontend into a jar.
   Params:
   * env - environment name
   * app-dir - str
   * deploy-to - keyword
   * deps-edn - map
   * exclude-aliases - set of aliases to exclude from deps-edn
   * as-lib - symbol/str how app package is referenced in deps
   * class-dir
   * jar-path - string with absolute path
   * jar-main - (optional) main entry point to run the jar
   * deploy-alias - keyword
   * main-css/custom-css/compiled-styles-css - (optional) css files paths for css compilation
   * license - (optional) map with :name and :url defining what license will be jar built with, jars can be built without license information, but some repositories require it (e.g. clojars)"
  [env
   app-dir
   deploy-to
   deps-edn
   exclude-aliases
   as-lib
   class-dir
   jar-path
   jar-main
   deploy-alias
   main-css
   custom-css
   compiled-styles-css
   license]
  (build-log/info "Start compilation")
  (let [paths (build-deps-edn/extract-paths deps-edn exclude-aliases)]
    (compile-frontend app-dir
                      deploy-alias
                      [main-css custom-css]
                      compiled-styles-css)
    (compile-backend deploy-to
                     app-dir
                     paths
                     as-lib
                     class-dir
                     jar-path
                     jar-main
                     env
                     license)))

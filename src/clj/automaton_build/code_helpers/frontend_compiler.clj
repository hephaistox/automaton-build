(ns automaton-build.code-helpers.frontend-compiler
  "Front end compiler toolings. Use shadow on npx."
  (:require
   [automaton-build.code-helpers.compiler.shadow :as build-compiler-shadow]
   [automaton-build.log                          :as build-log]
   [automaton-build.os.command                   :as build-cmd]
   [automaton-build.os.commands                  :as build-cmds]))

(defn compile-target
  "Compile the target given as a parameter, in dev mode
  Params:
  * `target-alias` the name of the alias in `shadow-cljs.edn` to compile
  * `dir` the frontend root directory"
  [target-alias dir]
  (when (build-compiler-shadow/shadow-installed? dir)
    (build-compiler-shadow/npm-install dir)
    (-> (build-cmds/execute-with-exit-code ["npx"
                                            "shadow-cljs"
                                            "compile"
                                            target-alias
                                            {:dir dir
                                             :error-to-std? true}])
        build-cmds/first-cmd-failing)))

(defn compile-release
  "Install npm, compile code for production.
  Order of those actions is important
  Params:
  * `target-alias` the name of the alias in `shadow-cljs.edn` to compile
  * `dir` the frontend root directory"
  [target-alias dir]
  (try (build-compiler-shadow/npm-install dir)
       (-> (build-cmds/execute-with-exit-code ["npx"
                                               "shadow-cljs"
                                               "release"
                                               target-alias
                                               {:dir dir
                                                :error-to-std? true}])
           build-cmds/first-cmd-failing)
       (catch Exception e
         (build-log/error (ex-info "Release compilation failed." e))
         nil)))

(defn builds
  "List shadow-cljs-build setup in the application
  Params:
  * `app-dir`"
  [app-dir]
  (some-> (build-compiler-shadow/load-shadow-cljs app-dir)
          (get :builds)
          keys
          vec))

(defn fe-test
  "Test the target frontend
  Return nil if successful
  Params:
  * `dir` the frontend root directory"
  [dir]
  (build-compiler-shadow/npm-install dir)
  (cond
    (not (and (build-compiler-shadow/is-shadow-project? dir)
              (build-compiler-shadow/shadow-installed? dir)))
    (do (build-log/trace
         "Frontend test is skipped as the project has no valid frontend")
        true)
    (not= :ok
          (build-cmd/log-if-fail
           (concat ["npx" "shadow-cljs" "compile"] (builds dir) [{:dir dir}])))
    (do (build-log/error "Compilation failed") true)
    :else (build-cmd/log-if-fail
           ["npx" "karma" "start" "--single-run" {:dir dir}])))

(defn create-size-optimization-report
  "Create a report on size-optimization
  Params:
  * `dir` the frontend root directory
  * `target-file` target file"
  [dir target-file]
  (build-log/debug "Generate the size optimization report in " target-file)
  (cond
    (not (build-compiler-shadow/is-shadow-project? dir))
    (build-log/debug "No frontend found, skip optimization report")
    (nil? (-> (build-compiler-shadow/load-shadow-cljs dir)
              (get-in [:build :app])))
    (build-log/debug "no app build target found, skip optimization report")
    :else (build-cmds/execute-and-trace ["npx"
                                         "shadow-cljs"
                                         "run"
                                         "shadow.cljs.build-report"
                                         "app"
                                         target-file
                                         {:dir dir}])))

(defn- shadow-cljs-watch-command
  [watch-aliases]
  (let [shadow-cljs-command ["npx" "shadow-cljs"]
        shadow-cljs-aliases (apply vector "watch" watch-aliases)
        shadow-cljs (-> shadow-cljs-command
                        (concat shadow-cljs-aliases)
                        vec)]
    shadow-cljs))

(defn fe-watch
  "Watch modification on code on cljs part, from tests or app
   Params:
   * `dir` the frontend root directory"
  [dir shadow-cljs-aliases]
  (build-compiler-shadow/npm-install dir)
  (let [shadow-cljs (shadow-cljs-watch-command shadow-cljs-aliases)]
    (build-cmds/execute-and-trace (conj shadow-cljs
                                        {:dir dir
                                         :error-to-std? true
                                         :background? true}))))

(defn extract-paths
  "Extract paths from the shadow cljs file content
  Params:
  * `shadow-cljs-content` is the content of a shadow-cljs file
  Return a flat vector of all source paths"
  [shadow-cljs-content]
  (:source-paths shadow-cljs-content))

(defn is-frontend-project?
  "Check if the frontend is a shadow project"
  [dir]
  (build-compiler-shadow/is-shadow-project? dir))

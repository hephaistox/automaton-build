(ns automaton-build.code-helpers.frontend-compiler
  "Front end compiler toolings. Currently use shadow on npx"
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files :as build-files]))

(def shadow-cljs-edn "shadow-cljs.edn")

(defn npm-install-cmd [dir] ["npm" "install" {:dir dir}])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn npm-audit-fix-cmd [dir] ["npm" "audit" "fix" {:dir dir}])

(defn npm-update [dir] ["npm" "update" {:dir dir}])

(defn is-shadow-project?
  [dir]
  (-> (build-files/create-file-path dir shadow-cljs-edn)
      build-files/is-existing-file?))

(defn npx-installed?*
  "Check if npx is installed
  Params:
  * `dir` where npx should be executed
  * `npx-cmd` (Optional, default=npx) parameter to tell the npx command"
  ([dir] (npx-installed?* dir "npx"))
  ([dir npx-cmd]
   (every? zero?
           (mapv first
                 (build-cmds/execute-with-exit-code
                  [npx-cmd "-v" {:dir dir}])))))

(def npx-installed? (memoize npx-installed?*))

(defn shadow-installed?*
  "Check if shadow-cljs is installed
  Params:
  * `dir` where to check if `shadow-cljs` is installed
  * `shadow-cmd` (Optional, default=shadow-cljs) parameter to tell the shadow cljs command
  * `npx-cmd` (Optional, default=npx) parameter to tell the npx command"
  ([dir] (shadow-installed?* dir "shadow-cljs" "npx"))
  ([dir shadow-cmd npx-cmd]
   (when (npx-installed? dir)
     (every? string?
             (build-cmds/execute-get-string
              [npx-cmd shadow-cmd "-info" {:dir dir}])))))

(def shadow-installed? (memoize shadow-installed?*))

(defn compile-target
  "Compile the target given as a parameter, in dev mode
  Params:
  * `target-alias` the name of the alias in `shadow-cljs.edn` to compile
  * `dir` the frontend root directory"
  [target-alias dir]
  (when (shadow-installed? dir)
    (-> (build-cmds/execute-with-exit-code (npm-install-cmd dir)
                                           ["npx"
                                            "shadow-cljs"
                                            "compile"
                                            target-alias
                                            {:dir dir
                                             :error-to-std? true}])
        build-cmds/first-cmd-failing)))

(defn- tailwind-compile-css
  [css-files compiled-dir]
  (let [tailwind-command ["npx" "tailwindcss"]
        input-files (apply vector "-i" css-files)
        output-files ["-o" compiled-dir]
        tailwindcss (-> tailwind-command
                        (concat input-files output-files)
                        vec)]
    tailwindcss))

(defn- tailwind-config-watch-command
  [css-files compiled-dir]
  (-> (tailwind-compile-css css-files compiled-dir)
      (concat ["--watch"])
      vec))

(defn tailwind-compile-css-release
  [css-files compiled-dir run-dir]
  (-> (tailwind-compile-css css-files compiled-dir)
      (concat ["--minify" {:dir run-dir}])
      vec))

(defn compile-release
  "Compile the target given as a parameter, in dev mode
  Params:
  * `target-alias` the name of the alias in `shadow-cljs.edn` to compile
  * `dir` the frontend root directory"
  [target-alias css-files output-css dir]
  (when (shadow-installed? dir)
    (-> (build-cmds/execute-with-exit-code
         (npm-install-cmd dir)
         (tailwind-compile-css-release css-files output-css dir)
         ["npx"
          "shadow-cljs"
          "release"
          target-alias
          {:dir dir
           :error-to-std? true}])
        build-cmds/first-cmd-failing)))


(defn load-shadow-cljs
  "Read the shadow-cljs of an app
  Params:
  * `app-dir` the directory of the application
  Returns the content as data structure"
  [app-dir]
  (let [shadow-filepath (build-files/create-file-path app-dir shadow-cljs-edn)]
    (when (build-files/is-existing-file? shadow-filepath)
      (build-edn-utils/read-edn shadow-filepath))))

(defn builds
  "List shadow-cljs-build setup in the application
  Params:
  * `app-dir`"
  [app-dir]
  (some-> (load-shadow-cljs app-dir)
          (get :builds)
          keys
          vec))

(defn fe-test
  "Test the target frontend
  Return nil if successful
  Params:
  * `dir` the frontend root directory"
  [dir]
  (if (and (is-shadow-project? dir) (shadow-installed? dir))
    (apply build-cmds/execute-and-trace
           (concat [(npm-install-cmd dir)]
                   (mapv
                    (fn [build]
                      ["npx" "shadow-cljs" "compile" (str build) {:dir dir}])
                    (builds dir))
                   [["npx" "karma" "start" "--single-run" {:dir dir}]]))
    true))

(defn create-size-optimization-report
  "Create a report on size-optimization
  Params:
  * `dir` the frontend root directory
  * `target-file` target file"
  [dir target-file]
  (build-log/debug "Generate the size optimization report in " target-file)
  (cond
    (not (is-shadow-project? dir))
    (build-log/debug "No frontend found, skip optimization report")
    (nil? (-> (load-shadow-cljs dir)
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
  [dir shadow-cljs-aliases css-files compiled-styles-css]
  (let [npm-install (npm-install-cmd dir)
        tailwindcss (tailwind-config-watch-command css-files
                                                   compiled-styles-css)
        shadow-cljs (shadow-cljs-watch-command shadow-cljs-aliases)]
    (build-cmds/execute-and-trace npm-install
                                  (conj tailwindcss
                                        {:dir dir
                                         :background? true})
                                  (conj shadow-cljs
                                        {:dir dir
                                         :background? true}))))

(defn extract-paths
  "Extract paths from the shadow cljs file content
  Params:
  * `shadow-cljs-content` is the content of a shadow-cljs file
  Return a flat vector of all source paths"
  [shadow-cljs-content]
  (:source-paths shadow-cljs-content))

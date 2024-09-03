(ns automaton-build.code-helpers.frontend-css
  "Front end compiler toolings. Currently use shadow on npx"
  (:require
   [automaton-build.code-helpers.compiler.shadow :as build-compiler-shadow]
   [automaton-build.os.commands                  :as build-cmds]
   [automaton-build.os.npm                       :as build-npm]))

(defn- tailwind-compile-css
  [css-file compiled-dir]
  (let [tailwind-command ["npx" "tailwindcss"]
        input-file ["-i" css-file]
        output-file ["-o" compiled-dir]
        tailwindcss (-> tailwind-command
                        (concat input-file output-file)
                        vec)]
    tailwindcss))

(defn- tailwind-config-watch-command
  [css-file compiled-dir]
  (-> (tailwind-compile-css css-file compiled-dir)
      (concat ["--watch"])
      vec))

(defn tailwind-compile-css-release
  [css-file compiled-dir run-dir]
  (-> (tailwind-compile-css css-file compiled-dir)
      (concat ["--minify" {:dir run-dir}])
      vec))

(defn compile-release
  "Install npm, compile code and css for production.
  Order of those actions is important
  Params:
  * `input-css-file` Tailwind allows only for one input file
  * `output-css`
  * `dir` the frontend root directory"
  [input-css-file output-css dir]
  (when (build-compiler-shadow/shadow-installed? dir)
    (-> (build-cmds/execute-with-exit-code
         (build-npm/npm-install-cmd dir)
         (tailwind-compile-css-release input-css-file output-css dir))
        build-cmds/first-cmd-failing)))

(defn fe-css-watch
  "Compile css and watch modification on it.
   Params:
   * `dir` the frontend root directory"
  [dir css-file compiled-styles-css]
  (let [npm-install (build-npm/npm-install-cmd dir)
        tailwindcss (tailwind-config-watch-command css-file compiled-styles-css)]
    (build-cmds/execute-and-trace npm-install
                                  (conj tailwindcss
                                        {:dir dir
                                         :background? true}))))

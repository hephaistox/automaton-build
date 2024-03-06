(ns automaton-build.code-helpers.compiler.shadow
  "Proxy to shadow-cljs compiler

  Currently use shadow on npx"
  (:require
   [automaton-build.os.command :as build-cmd]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files :as build-files]
   [automaton-build.os.npm :as build-npm]))

(def ^:private shadow-cljs-edn "shadow-cljs.edn")

(defn- shadow-installed?*
  "Check if shadow-cljs is installed
  Params:
  * `dir` where to check if `shadow-cljs` is installed"
  [dir]
  (let [shadow-cmd "shadow-cljs"
        npx-cmd "npx"]
    (when (build-npm/npx-installed? dir)
      (every? string?
              (build-cmds/execute-get-string
               [npx-cmd shadow-cmd "-info" {:dir dir}])))))

(def shadow-installed?
  "Is shadow installed on that project?
  This function is efficient (memoize results)"
  (memoize shadow-installed?*))

(defn npm-install
  "Install the packages defined in `package.json` and version in `package-lock.json`"
  [dir]
  (build-cmd/log-if-fail ["npm" "install" {:dir dir}]))

(defn is-shadow-project?
  "Returns true if the project is shadow"
  [dir]
  (-> (build-files/create-file-path dir shadow-cljs-edn)
      build-files/is-existing-file?))

(defn load-shadow-cljs
  "Read the shadow-cljs of an app
  Params:
  * `app-dir` the directory of the application
  Returns the content as data structure"
  [app-dir]
  (let [shadow-filepath (build-files/create-file-path app-dir shadow-cljs-edn)]
    (when (build-files/is-existing-file? shadow-filepath)
      (build-edn-utils/read-edn shadow-filepath))))

(ns automaton-build.adapters.frontend-compiler
  "Gather front end compilers toolings. Currently use shadow on npx "
  (:require
   [automaton-build.adapters.commands :as cmds]
   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-core.env-setup :as env-setup]))

(defn npx-installed?*
  "Check if npx is installed
  Params:
  * `dir` where npx should be executed
  * `npx-cmd` (Optional, default=npx) parameter to tell the npx command"
  ([dir]
   (npx-installed?* dir "npx"))
  ([dir npx-cmd]
   (try
     (cmds/exec-cmds [[[npx-cmd "-v"]]]
                     {:out :string
                      :dir dir})
     (catch clojure.lang.ExceptionInfo e
       (throw (ex-info (format "npx is not working, aborting, command `%s` has failed" npx-cmd)
                       {:exception e
                        :npx-cmd npx-cmd}))))))

(def npx-installed?
  (memoize npx-installed?*))

(defn shadow-installed?*
  "Check if shadow-cljs is installed
  Params:
  * `dir` where to check if `shadow-cljs` is installed
  * `shadow-cmd` (Optional, default=shadow-cljs) parameter to tell the shadow cljs command
  * `npx-cmd` (Optional, default=npx) parameter to tell the npx command"
  ([dir]
   (shadow-installed?* dir "shadow-cljs" "npx"))
  ([dir shadow-cmd npx-cmd]
   (npx-installed? npx-cmd)
   (try
     (cmds/exec-cmds [[[npx-cmd shadow-cmd "-info"]]]
                     {:out :string
                      :dir dir})
     (catch clojure.lang.ExceptionInfo e
       (throw (ex-info (format "shadow is not working, aborting, command `%s` has failed" shadow-cmd)
                       {:exception e
                        :npx-cmd shadow-cmd}))))))

(def shadow-installed?
  (memoize shadow-installed?*))

(defn compile-target
  "Compile the target given as a parameter, in dev mode
  Params:
  * `npx-cmd` optional parameter to tell the npx command
  * `target-alias` the name of the alias in `shadow-cljs.edn` to compile
  * `dir` the frontend root directory"
  [npx-cmd target-alias dir]
  (shadow-installed? dir)
  (log/trace (cmds/exec-cmds [[["npm" "install"]]
                              [[npx-cmd "shadow-cljs" "compile" target-alias]]]
                             {:dir dir
                              :out :string})))

(defn test-fe
  "Test the target frontend
  Params:
  * `dir` the frontend root directory"
  [dir]
  (shadow-installed? dir)
  (log/trace (cmds/exec-cmds [[["npm" "install"]]
                              [["npx" "shadow-cljs" "compile" "karma-test"]]
                              [["npx" "karma" "start" "--single-run"]]]
                             {:dir dir
                              :out :string})))

(defn create-size-optimization-report
  "Create a report on size-optimization
  Params:
  * `dir` the frontend root directory"
  [dir]
  (let [target-file (files/create-file-path (get-in env-setup/env-setup
                                                    [:documentation :code-subdir])
                                            "build-report.html")]
    (log/debug "Generate the size optimization report in " target-file)
    ;; The output of the command is useless, as it is pushed in the targetfile
    (cmds/exec-cmds [[["npx" "shadow-cljs" "run" "shadow.cljs.build-report" "app" target-file]]]
                    {:dir dir
                     :out :string})))

(defn watch-modifications
  "Watch modification on code on cljs part, from tests or app
   Params:
   * `dir` the frontend root directory"
  [dir]
  (let [main-css (files/create-file-path "resources" "css" "main.css")
        compiled-main-css (files/create-file-path "resources" "public" "css" "compiled" "main.css")]
    (cmds/exec-cmds [[["npm" "install"]]
                     [["npx" "tailwindcss" "-i" main-css "-o" compiled-main-css "--watch"] {:blocking? false}]
                     [["npx" "shadow-cljs" "watch" "app" "karma-test" "browser-test"]]]
                    {:dir dir})))

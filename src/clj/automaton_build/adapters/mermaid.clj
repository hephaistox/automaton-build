(ns automaton-build.adapters.mermaid
  "To create mermaid images
  Proxy to cli mermaid"
  (:require
   [clojure.string :as str]

   [automaton-build.adapters.commands :as cmds]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.log :as log]))

(defn need-to-update?
  "Does the `file-in` needed to be modified?
  Params:
  * `file-in` file used as input, returns true if that source has been modified after the file generation
  * `file-out` file used as output"
  [file-in file-out]
  (and (str/ends-with? file-in ".mermaid")
       (seq (files/modified-since file-out [file-in]))))

(defn build-a-file
  "Launch mermaid to build the `svg` image of that file
  Params
  * `file-in` input mermaid file"
  [file-in]
  (let [file-out (files/change-extension file-in ".svg")]
    (when (need-to-update? file-in file-out)
      (log/trace "Compile mermaid `" file-in "`, to `" file-out "`")
      (log/trace (cmds/exec-cmds [[["mmdc" "-i" file-in "-o" file-out]]]
                                 {:dir "."
                                  :out :string})))))

(defn build-all-files*
  "Build all mermaid files in the directory `archi-dir`
  Params:
  * `archi-dir` Directory where all `.mermaid` extension files are turned into `.svg` files"
  [archi-dir]
  (files/for-each archi-dir
                  build-a-file))

(defn build-all-files
  "Scan all apps and build
  Params:
  * `archi-dir` Directory where all `.mermaid` extension files are turned into `.svg` files"
  [archi-dir]
  (log/debug "Build if needed all files in `" archi-dir "`")
  (build-all-files* archi-dir))

(defn watch
  "Watch the docs directory to build mermaid images
  * `archi-dir` is the directory to watch"
  [archi-dir]
  (log/info "Start watching docs directory in " archi-dir)
  (loop []
    (build-all-files* archi-dir)
    (Thread/sleep 1000)
    (recur)))

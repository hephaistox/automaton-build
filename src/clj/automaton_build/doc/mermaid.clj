(ns automaton-build.doc.mermaid
  "To create mermaid images
  Proxy to cli mermaid"
  (:require
   [automaton-build.log         :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.files    :as build-files]))

(def ^:private mermaid-pattern "**.mermaid")

(defn need-to-update?
  "Does the `file-in` needed to be modified?
  Params:
  * `file-in` file used as input, returns true if that source has been modified after the file generation
  * `file-out` file used as output"
  [file-in file-out]
  (and (build-files/match-extension? file-in ".mermaid")
       (seq (build-files/modified-since file-out [file-in]))))

(defn build-a-file
  "Launch mermaid to build the `svg` image of that file
  Params
  * `file-in` input mermaid file"
  [file-in]
  (when (build-files/is-existing-file? file-in)
    (let [file-out (build-files/change-extension file-in ".svg")]
      (when (need-to-update? file-in file-out)
        (build-log/trace-format "Compile mermaid `%s`, to `%s`"
                                file-in
                                file-out)
        (build-cmds/execute-and-trace
         ["mmdc" "-i" file-in "-o" file-out {:dir "."}])))))

(defn build-all-files*
  "Build all mermaid files in the directory `archi-dir`
  Params:
  * `archi-dir` Directory where all `.mermaid` extension files are turned into `.svg` files"
  [archi-dir]
  (build-files/for-each archi-dir mermaid-pattern build-a-file))

(defn build-all-files
  "Scan all apps and build
  Params:
  * `dir` Directory and subdir where all `.mermaid` extension files are turned into `.svg` files"
  [dir]
  (build-log/debug-format "Build mermaid files in `%s`" dir)
  (let [res (build-all-files* dir)] (every? some? res)))

(defn watch
  "Watch the docs directory to build mermaid images
  * `dir` is the directory to watch"
  [dir]
  (build-log/info-format "Start watching mermaid files in directory `%s` " dir)
  (loop []
    (build-all-files* dir)
    (Thread/sleep 1000)
    (recur)))

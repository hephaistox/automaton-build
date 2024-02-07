(ns automaton-build.code-helpers.lint
  "Code linter"
  (:require
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.files :as build-files]
   [automaton-build.log :as build-log]))

(defn- lint-cmd
  "Lint command"
  [debug? paths]
  (concat ["clj-kondo" "--lint"] paths (when debug? ["--debug"])))

(defn lint-files
  "Lint all files to make sure they are compliant with our styling standards
  Params:
  * `debug?`
  * `files` is a seq of file paths"
  [debug? files]
  (build-cmds/execute-and-trace (lint-cmd debug? files)))

(defn lint-dirs
  "Lint the code
  Params:
  * `debug?`
  * `dirs` list of dirs to lint"
  [debug? dirs]
  (let [dirs (filterv build-files/is-existing-dir? dirs)]
    (if (empty? dirs)
      (do (build-log/warn "No file to lint, no class paths found") true)
      (build-cmds/execute-and-trace (lint-cmd debug? dirs)))))

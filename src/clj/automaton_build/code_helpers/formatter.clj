(ns automaton-build.code-helpers.formatter
  "Format code
  Proxy to [zprint](https://github.com/kkinnear/zprint)"
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.files :as build-files]
   [automaton-build.cicd.server :as build-cicd-server]))

(def ^:private use-local-zprint-config-parameter #":search-config\?\s*true")

(def ^:private zprint-file "~/.zprintrc")

(defn- is-formatter-setup
  "As described in the [zprint documentation](https://github.com/kkinnear/zprint/blob/main/doc/using/project.md#use-zprint-with-different-formatting-for-different-projects),
Params:
  * `none`"
  []
  (if (or (build-cicd-server/is-cicd?)
          (not (some->> (build-files/read-file zprint-file)
                        (re-find use-local-zprint-config-parameter))))
    (do
      (build-log/warn-format
       "Formatting aborted as the formatter setup must include `%s`\n Please add it to `%s` file"
       use-local-zprint-config-parameter
       zprint-file)
      false)
    true))

(defn format-file
  "Format the `clj` or `edn` file

  Returns nil if something is wrong, the exit code otherwise
  Params:
  * `filename` to format
  * `header` (optional) is written at the top of the file"
  [filename]
  (cond
    (not (is-formatter-setup)) nil
    (build-files/is-existing-file? filename)
    (first (build-cmds/execute-and-trace-return-exit-codes
            ["zprint" "-w" filename]))
    :else (do (build-log/warn-format "Can't format file `%s` as it's not found"
                                     filename)
              nil)))

(defn files-formatted
  "Formats all files to make sure they are compliant with our styling standards
  Returns true if all files have successfully updated

  Params:
  * `files` is a seq of file paths"
  [files]
  (when (is-formatter-setup)
    (let [formattings (map format-file files)]
      (build-log/info "Files formatted")
      (every? some? formattings))))

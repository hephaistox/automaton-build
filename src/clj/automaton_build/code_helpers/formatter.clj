(ns automaton-build.code-helpers.formatter
  "Format code
  Proxy to [zprint](https://github.com/kkinnear/zprint)"
  (:require
   [automaton-build.cicd.server :as build-cicd-server]
   [automaton-build.log         :as build-log]
   [automaton-build.os.command  :as build-cmd]
   [automaton-build.os.files    :as build-files]))

(def ^:private use-local-zprint-config-parameter #":search-config\?\s*true")

(def ^:private zprint-file "~/.zprintrc")

(defn- is-formatter-setup
  "As described in the [zprint documentation](https://github.com/kkinnear/zprint/blob/main/doc/using/project.md#use-zprint-with-different-formatting-for-different-projects),

  Returns true if zprint is ready to execute

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
  Format file is not blocking if the formatter is not setup, if the file does not exist

  Returns nil if successfully updated
  Params:
  * `filename` to format"
  [filename]
  (cond
    (not (is-formatter-setup)) nil
    (not (build-files/is-existing-file? filename))
    (do (build-log/trace-format "Can't format file `%s` as it's not found"
                                filename)
        nil)
    (= :ok (build-cmd/log-if-fail ["zprint" "-w" filename])) nil
    :else (do (build-log/trace-format "Execution of zprint failed") true)))

(defn files-formatted
  "Formats all files to make sure they are compliant with our styling standards

  Returns nil if all files have successfully updated
  Is non blocking if the formatter is not setup (as formatting is not setup on CICD)

  Params:
  * `files` is a seq of file paths"
  [files]
  (when (is-formatter-setup)
    (let [formattings (map format-file files)]
      (build-log/info "Files formatted")
      (every? nil? formattings))))

(defn format-clj
  "Format all clj files in a dir
  Returns true if successfully updated

  Params:
  * `dir` directory where to find"
  [dir]
  (= :ok
     (build-cmd/log-if-fail ["fd" "-e" "clj" "-x" "zprint" "-w" {:dir dir}])))

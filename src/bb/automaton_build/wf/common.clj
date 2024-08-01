(ns automaton-build.wf.common
  "All common workflow features."
  (:require
   [automaton-build.echo.common    :as build-echo-common]
   [automaton-build.os.cli-opts    :as build-cli-opts]
   [automaton-build.project.config :as build-project-config]))

(def app-dir "")

(defn project-config
  "Load the content of the `build_config.edn` file."
  []
  (let [build-data-project-filename (build-project-config/filename app-dir)]
    (if-let [build-data-config (build-project-config/read
                                build-data-project-filename)]
      build-data-config
      (build-echo-common/errorln
       "Missing a valid project configuration file."))))

(defn print-cmd
  "Print the command `cmd` in the CLI."
  [cmd]
  (build-echo-common/print-cmd "" cmd))

(defn process-result-errors
  "Display the process-result to tell errors."
  [process-result]
  (when-let [{:keys [exit-code]} process-result]
    (build-echo-common/errorln "Unexpected exit-code : " exit-code))
  (when-let [e (:exception process-result)]
    (build-echo-common/errorln "Unexpected exception : ")
    (build-echo-common/exceptionln e)))

(defn enter
  "When entering the task:

  * Print usage if required.
  * Print options if required."
  [cli-opts current-task]
  (when-let [message (build-cli-opts/usage cli-opts (:name current-task))]
    (println message)
    (System/exit 1)))

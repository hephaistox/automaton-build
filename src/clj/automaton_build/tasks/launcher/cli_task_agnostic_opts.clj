(ns automaton-build.tasks.launcher.cli-task-agnostic-opts
  "All tasks agnostic options - cli options that are necessary for all tasks

  Decision:
  * All common cli tasks (the cli options available for all tasks) are processed here
  * That options are called only once, at the beginning of the bb tasks, the task related cli options are in a different namespaces"
  (:require
   [automaton-build.log :as build-log]
   [clojure.tools.cli   :refer [parse-opts]]))

(defn cli-common-opts
  []
  [["-l"
    "--log LOG-LEVEL"
    "Log level, one of `trace`, `debug`, `info`, `warning`, `error`, `fatal`"
    :default
    :info
    :parse-fn
    keyword
    :validate
    [(partial contains? #{:trace :debug :fatal :warning :info :error})
     "Must be one of `trace`, `debug`, `info`, `warning`, `error`, `fatal`"]]
   ["-d" "--details" "Show details and don't ellipsis the log"]
   ["-h" "--help" "Displays this help message"]])

(defn- print-help-message
  "Print the help message
  Params:
  * `task-name`
  * `cli-opts`"
  [task-name cli-opts]
  (println (format "`bb %s` usage" task-name))
  (println (:summary cli-opts)))

(defn common-opts!
  "Set log levels and if lines are ellipsis or details shown.
  Returns true if a common action is executed, nil otherwise

  Params:
  * `cli-opts` options returned by the cli"
  [cli-args task-name]
  (let [{:keys [options]
         :as cli-opts}
        (parse-opts cli-args (cli-common-opts))
        {:keys [log details help]} options]
    (build-log/set-min-level! log)
    (build-log/set-details? details)
    (build-log/print-log-parameters)
    (if help (do (print-help-message task-name cli-opts) true) nil)))

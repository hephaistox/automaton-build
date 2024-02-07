(ns automaton-build.tasks.launcher.cli-task-opts
  "Cli options for tasks"
  (:require
   [clojure.string :as str]
   [automaton-build.tasks.launcher.cli-task-agnostic-opts
    :as
    build-cli-task-agnostic-opts]
   [automaton-build.os.terminal-msg :as build-terminal-msg]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.set :as set]
   [automaton-build.log :as build-log]))

(defn- cli-opts-spec-register
  "Register container specs of tasks dependent cli options"
  []
  {:force {:spec [["-f" "--force" "Force execution on local machine"]]}
   :tag {:spec [["-t" "--tag TAG-MESSAGE" "Tag for the publication"]]
         :mandatory? true}
   :environment {:spec [["-e"
                         "--environment ENV-NAME"
                         "Environment name for the publication"]]
                 :mandatory? true}
   :message {:spec
             [["-M" "--message COMMIT-MESSAGE" "Mandatory: Commit message"]]
             :mandatory? true}
   :message-opt {:spec
                 [["-m" "--message-opt COMMIT-MESSAGE" "Commit message"]]}})

(defn- print-error-message
  [cli-opts msg]
  (build-terminal-msg/println-msg (format (cond
                                            (str/blank? msg)
                                            "The cli arguments are invalid"
                                            :else msg)))
  (some-> (:errors cli-opts)
          println)
  (build-terminal-msg/println-msg (:summary cli-opts)))

(defn are-cli-opts-valid?
  "Print the error message, tell what's expected
  Returns `true` if valid, `false` otherwise
  Params:
  * `cli-opts` a map of cli options
  * `msg` message to display if an error occur"
  [cli-opts msg]
  (if (:errors cli-opts) (do (print-error-message cli-opts msg) false) true))

(defn mandatory-option-present?
  "Print an error message if the option at address `ks` is not found in `cli-opts`
  Returns `false` if some mandatory options are missing
  Design decision:
  * It is a choice not to use log just to be sure it is printed on the terminal, without any ellipsis and prefix
  Params:
  * `cli-opts`
  * `ks` sequence of keyword to tell the path of the searched value"
  [cli-opts cli-opts-spec-kws]
  (let [mandatory-cli-args (set/intersection
                            (->> (cli-opts-spec-register)
                                 (keep (fn [[cli-opt-kw cli-opt-map]]
                                         (when (:mandatory? cli-opt-map)
                                           cli-opt-kw)))
                                 set)
                            (set cli-opts-spec-kws))
        found-cli-args (-> (:options cli-opts)
                           keys
                           set)
        missing-cli-args (set/difference mandatory-cli-args found-cli-args)]
    (build-log/trace-format "Mandatory cli args for that task are %s"
                            mandatory-cli-args)
    (when-not (empty? missing-cli-args)
      (build-log/trace-format "The following cli args are found %s"
                              found-cli-args)
      (build-log/debug-format "These cli-args are mandatory and missing: `%s`"
                              missing-cli-args))
    (doseq [cli-arg missing-cli-args]
      (print-error-message
       cli-opts
       (format "The cli argument `%s` is mandatory and missing! " cli-arg)))
    (not (pos? (count missing-cli-args)))))

(defn cli-opts-spec
  "For given `cli-opts-spec-kws`, creates the cli options specifications
  Params:
  * `cli-opts-spec-kws` collection of name of the options to build the cli opt spec for"
  [cli-opts-spec-kws]
  (let [cli-opts-spec (cli-opts-spec-register)
        common-specs (build-cli-task-agnostic-opts/cli-common-opts)
        specific-specs (mapcat #(get-in cli-opts-spec [% :spec])
                        cli-opts-spec-kws)]
    (-> (concat common-specs specific-specs)
        vec)))

(defn cli-opts
  "Get the cli-opts from the spec
  Params:
  * `task-cli-opts-kws` keyword collection of the option to build the cli opt spec for
  * `cli-args` arguments of the cli as in `*command-line-args*`"
  [task-cli-opts-kws cli-args]
  (let [cli-opts-specs (cli-opts-spec task-cli-opts-kws)]
    (parse-opts cli-args cli-opts-specs)))

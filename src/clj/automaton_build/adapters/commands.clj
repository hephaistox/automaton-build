(ns automaton-build.adapters.commands
  "Library to execute a set of commands"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]

   [babashka.process :as p]

   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.log :as log]
   [automaton-build.adapters.schema :as schema]
   [automaton-build.adapters.string :as string]
   [automaton-build.adapters.edn-utils :as edn-utils]))

(def size-command
  "Size of the command line to be managed, measured on mcbook pro"
  185)

(def commands-schema
  [:vector [:or
            [:tuple
             [:vector [:string]]
             :map]
            [:tuple
             [:vector [:string]]]]])

(def glob-command-params
  "Command params globals"
  {:in :inherit
   :blocking? true})

(defn execute-command
  "Execute a command, described with
  Params:
  * `command` - tokenize command
  * `cmd-params` list of parameters for that particular command
  * `cmds-params` list of parmeters set once for the global command"
  [[command cmd-params] cmds-params]
  (let [merged-params (merge glob-command-params
                             cmd-params
                             cmds-params)
        execute-command-params {:command command
                                :merged-params merged-params
                                :cmd-params cmd-params
                                :cmds-params cmds-params}
        {:keys [dir out in blocking?]} merged-params
        filename out
        str-command (str/join " " command)
        file (if (or (nil? out)
                     (= out :string))
               nil
               (io/file filename))
        out (cond (= out :string) :string
                  out :write
                  :else :inherit)
        blocking? (or (= out :string)
                      blocking?)]

    (files/create-dirs dir)
    (log/debug (string/limit-length str-command
                                    size-command
                                    "`"
                                    (str  "` in directory : `" (files/absolutize dir))
                                    (fn [msg]
                                      (log/trace (edn-utils/spit-in-tmp-file msg)))))
    (when (and (not= out :string)
               filename)
      (log/info "  -> output = " filename))

    (try
      (let [proc (p/process command
                            {:dir dir
                             :shutdown p/destroy-tree
                             :out out
                             :out-file file
                             :err out
                             :err-file file
                             :in in})]
        (if blocking?
          (p/check proc)
          proc))
      (catch java.io.IOException e
        (throw (ex-info (str "Directory does not exist")
                        (merge {:exception e}
                               execute-command-params))))
      (catch clojure.lang.ExceptionInfo e
        (let [{:keys [exit type]} (ex-data e)]
          (if (= type :babashka.process/error)
            (throw (ex-info (str "Command `" str-command "` failed on exit code " exit)
                            (merge execute-command-params
                                   {:exception e})))
            (throw (ex-info (str "Command `" str-command "` failed ")
                            (merge execute-command-params
                                   {:exception e}))))))
      (catch Exception e
        (throw (ex-info (str "Command `" str-command "` failed ")
                        (merge execute-command-params
                               {:exception e})))))))

(defn exec-cmds
  "Execute commands with their default parameters
   is a vector of two elements:
   * the first is a vector of string, each representing an argument on the cli
   * the second is the set of parameters, which allow to be specific for each command
  Params:
  * `commands` tokenize command"
  ([commands]
   (exec-cmds commands {}))
  ([commands default-params]
   (schema/schema-valid-or-throw commands-schema
                                 commands
                                 "Malformed command")
   (apply str
          (doall
           (for [command commands]
             (:out
              (execute-command command default-params)))))))

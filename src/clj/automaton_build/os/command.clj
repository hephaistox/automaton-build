(ns automaton-build.os.command
  "Execute a process
  Is a proxy for `babashka.process`"
  (:require
   [automaton-build.log            :as build-log]
   [automaton-build.os.command.str :as build-cmd-str]
   [babashka.process               :as babashka-process]
   [clojure.string                 :as str]))

(defn- generate-process
  [command]
  (let [updated-command (build-cmd-str/add-opts command
                                                {:out :string
                                                 :err :string})
        command-tokens (build-cmd-str/cmd-tokens updated-command)
        opts (build-cmd-str/opts updated-command)]
    (build-log/trace-format "Execute `%s` with options = `%s`"
                            (str/join " " command-tokens)
                            (pr-str opts))
    (apply babashka-process/process opts command-tokens)))

(defn log-if-fail
  [command]
  (try (let [{:keys [exit out err]} @(generate-process command)]
         (if (zero? exit)
           :ok
           (do (build-log/error (str err)) (build-log/info (str out)) :fail)))
       (catch Exception e
         (build-log/error-exception e)
         (build-log/error-format
          "Unexpected error during execution of this command `%s`"
          command))))

(ns automaton-build.wf.4
  "Prepare project for proper commit."
  (:refer-clojure :exclude [format])
  (:require
   [automaton-build.code.formatter :as build-code-formatter-bb]
   [automaton-build.echo.headers   :as build-echo-headers]
   [automaton-build.os.cli-opts    :as build-cli-opts]
   [automaton-build.os.cmds        :as build-commands]
   [automaton-build.wf.common      :as build-wf-common]))

(def format-opts ["-f" "--format" "Don't format" :default true :parse-fn not])

(def cli-opts
  (-> [format-opts]
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              build-cli-opts/log-options)
      build-cli-opts/parse-cli))

(defn format
  "Format project files."
  []
  (when (get-in cli-opts [:options :format])
    (let [format-cmd (build-code-formatter-bb/format-clj-cmd)]
      (build-wf-common/print-cmd format-cmd)
      (build-commands/blocking-cmd format-cmd))))

(defn run [] (build-echo-headers/errorln "Not implemented yet."))

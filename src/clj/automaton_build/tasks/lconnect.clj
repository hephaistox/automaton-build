(ns automaton-build.tasks.lconnect
  (:require
   [automaton-build.log         :as build-log]
   [automaton-build.os.commands :as build-cmds]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn lconnect
  "Local connection to the code"
  [_cli-opts
   {:keys [build-config]
    :as _app}
   _bb-edn-args]
  (let [aliases (get-in build-config [:lconnect :aliases])]
    (build-log/info-format "Starting repl with aliases `%s`"
                           (apply str aliases))
    (build-cmds/execute-and-trace ["clojure" (apply str "-M" aliases)])))

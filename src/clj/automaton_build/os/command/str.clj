(ns automaton-build.os.command.str
  "Manipulates strings of command"
  (:require
   [babashka.process :as babashka-process]
   [clojure.string :as str]))

(def default-opts
  {:in :inherit
   :out :inherit
   :shutdown babashka-process/destroy-tree})

(defn cmd-tokens
  "Returns tokens from the command (exclude option)"
  [cmd]
  ((fnil identity []) (if (map? (last cmd)) (butlast cmd) cmd)))

(defn opts
  "Returns opts from the command (even if none is defined)"
  [cmd]
  (-> default-opts
      (merge (if (map? (last cmd)) (last cmd) {}))
      (update :dir #(if (str/blank? %) "." %))))

(defn add-opts
  "Adds the `opts` in the command `cmd`
  Params:
  * `cmd` command
  * `h-opts` higher priority options compared to existing ones"
  [cmd h-opts]
  (vec (concat (cmd-tokens cmd) [(merge (opts cmd) h-opts)])))

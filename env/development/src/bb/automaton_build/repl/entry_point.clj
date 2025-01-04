(ns automaton-build.repl.entry-point
  "Entry point for repl"
  (:require
   [nrepl.server :refer [start-server]]))

(defn -main
  "Entry point"
  [& _args]
  (let [port 1234]
    (println "Automaton build development mode")
    (println "Start repl on port" port)
    (start-server :port port)))

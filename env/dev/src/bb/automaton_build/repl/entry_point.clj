(ns automaton-build.repl.entry-point
  "Entry point for repl"
  (:require
   [cider.nrepl.middleware :as mw]
   [nrepl.server           :refer [default-handler start-server]]
   [refactor-nrepl.middleware]))

(def custom-nrepl-handler
  "We build our own custom nrepl handler, mimicking CIDER's."
  (apply default-handler
         (conj cider.nrepl.middleware/cider-middleware 'refactor-nrepl.middleware/wrap-refactor)))

(defn -main
  "Entry point"
  [& _args]
  (let [port 1234]
    (println "Automaton build development mode")
    (println "Start repl on port" port)
    (start-server :port port :handler custom-nrepl-handler (spit ".nrepl-port" port))))

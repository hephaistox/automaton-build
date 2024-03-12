#_{:heph-ignore {:forbidden-words ["automaton-core"]}}
(ns automaton-build.repl
  "REPL component
  Design decision:
  * This REPL is for build-app only, all other Hephaistox REPL should use the `automaton-core` version
  * The REPL could have pushed to dev, but leaving it here allows to remotely connect to the remote repl, like la or production"
  (:require
   [clojure.core.async :refer [<!! chan]]))

(defn -main
  "Entry point for simple"
  [& _args]
  (let [c (chan)]
    (require '[automaton-build.repl.launcher])
    ((resolve 'automaton-build.repl.launcher/start-repl))
    (<!! c)))

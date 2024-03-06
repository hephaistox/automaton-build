#_{:heph-ignore {:forbidden-words ["automaton-core"]}}
(ns automaton-build.repl.entry-point
  "REPL component
  Design decision:
  * This REPL is for build-app only, all other Hephaistox REPL should use the `automaton-core` version
  * The REPL could have pushed to dev, but leaving it here allows to remotely connect to the remote repl, like la or production"
  (:require
   [clojure.core.async :refer [<!! chan]]
   [cider.nrepl :as cider-nrepl]
   [refactor-nrepl.middleware]))

(defn -main
  "Entry point for simple"
  [& _args]
  (let [c (chan)]
    (require '[automaton-build.repl.launcher])
    ((resolve 'automaton-build.repl.launcher/start-repl)
     (conj cider-nrepl/cider-middleware
           'refactor-nrepl.middleware/wrap-refactor))
    (<!! c)))

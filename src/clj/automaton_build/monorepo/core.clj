(ns automaton-build.monorepo.core
  "Entry point for monorepo bb tasks"
  (:require
   [automaton-build.core :as bc]

   [automaton-build.monorepo.tasks :as tasks]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run
  "Main entry point for clj -T:build of build monorepo"
  [cli-params]
  (let [tasks tasks/tasks]
    (bc/run (bc/create-apps)
            cli-params
            tasks)))

(ns automaton-build.tasks.registry.conditions
  "Namespace gathering concepts of conditions to display tasks. This concept has special namespace to not pollute display of tasks with to many requires.
  All functions should expect app-data - map with application config."
  (:require
   [automaton-build.cicd.server :as build-cicd-server]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn not-deploy-target?
  "Is deploy-to keyword not in app publication?"
  [app-data]
  (nil? (get-in app-data [:publication :deploy-to])))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn not-cicd? [_app-data] (not (build-cicd-server/is-cicd?)))

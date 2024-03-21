(ns automaton-build.tasks.lfe-manual
  (:require
   [automaton-build.os.cli-input  :as build-cli-input]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn- lfe-tests-successful?
  "Ask user if the frontend tests are successful"
  [force?]
  (build-cli-input/yes-question
   (format
    "Are your frontend tests passing? (You can check them in your browser after starting wf-2)")
   force?))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [force]}]
  (if (lfe-tests-successful? force)
    build-exit-codes/ok
    build-exit-codes/catch-all))

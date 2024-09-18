(ns automaton-build.tasks.lbe-repl
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [babashka.process              :as babashka-process]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [repl-aliases]}]
  (try (future (babashka-process/shell "clojure" (apply str "-M:" repl-aliases)))
       build-exit-codes/ok
       (catch Exception e (build-log/error-exception e) build-exit-codes/cannot-execute)))

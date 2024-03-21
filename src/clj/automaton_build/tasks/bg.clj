(ns automaton-build.tasks.bg
  (:require
   [automaton-build.log :as build-log]
   [clojure.core.async  :refer [<!! chan]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Pause the execution - usefull for tasks that need to continue their execution in bg, like watchers"
  [_task-map _app-data]
  (build-log/info "Will block until you break its execution")
  (let [c (chan 1)] (<!! c)))


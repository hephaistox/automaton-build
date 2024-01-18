(ns automaton-build.doc.visualize-ns
  "Visualize namespaces
  Proxy to io.dominic.vizns.core"
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]
   [io.dominic.vizns.core :as vizns]))

(defn visualize-ns
  "Visualize all namespaces relations"
  [deps-filename]
  (build-log/info "Graph of ns - deps link")
  (build-log/trace-format "Graph stored in `%s`" deps-filename)
  (build-files/create-parent-dirs deps-filename)
  (try (vizns/-main "single" "-o" deps-filename "-f" "svg")
       true
       (catch Exception e
         (build-log/error "Unexpected error during execution of vizns")
         (build-log/trace-exception e)
         false)))

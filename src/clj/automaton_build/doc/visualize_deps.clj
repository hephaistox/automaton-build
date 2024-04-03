(ns automaton-build.doc.visualize-deps
  "Build the dependencies
  Proxy to tools deps"
  (:require
   [automaton-build.log      :as build-log]
   [automaton-build.os.files :as build-files]))

(defn visualize-deps
  "Visualize the dependencies between deps"
  [output-filename]
  (build-log/info "Graph of deps")
  (build-log/trace-format "Graph stored in `%s`" output-filename)
  (build-files/create-parent-dirs output-filename)
  (build-log/debug "Skip as it is not working yet")
  #_(build-cmds/execute-and-trace
     ["clj" "-Tgraph" "graph" ":output" output-filename])
  true)

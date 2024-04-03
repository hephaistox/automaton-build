(ns automaton-build.tasks.docstring
  (:require
   [automaton-build.app-data      :as build-app-data]
   [automaton-build.doc.docstring :as build-docstring]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map
   {:keys [app-dir app-name description dir exclude-dirs title]
    :as app-data}]
  (let [app-dirs (->> (build-app-data/classpath-dirs app-data)
                      (filterv #(not (contains? exclude-dirs %))))]
    (if (true? (build-docstring/docstring app-dir
                                          app-name
                                          app-dirs
                                          title
                                          description
                                          dir))
      build-exit-codes/ok
      build-exit-codes/catch-all)))

(ns automaton-build.tasks.storage-start
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.storage :as build-storage]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [clojure.string :as str]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [storage-datomic]}]
  (let [{:keys [datomic-root-dir
                datomic-dir-pattern
                datomic-ver
                datomic-transactor-bin-path]}
        storage-datomic]
    (build-log/info-format "Storage is getting started (datomic v%s)"
                           datomic-ver)
    (if (str/blank? datomic-ver)
      (build-log/warn "Parameter datomic-ver is missing in build_config.edn")
      (if-not (build-storage/run datomic-root-dir
                                 datomic-dir-pattern
                                 datomic-transactor-bin-path
                                 datomic-ver)
        build-exit-codes/catch-all
        build-exit-codes/ok))))

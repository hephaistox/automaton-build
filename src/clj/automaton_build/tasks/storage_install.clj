(ns automaton-build.tasks.storage-install
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.storage       :as build-storage]
   [clojure.string                :as str]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [datomic-url-pattern storage-datomic force]}]
  (let [{:keys [datomic-root-dir
                datomic-dir-pattern
                datomic-ver
                datomic-transactor-bin-path]}
        storage-datomic]
    (build-log/info-format "Storage is setup (datomic version %s)" datomic-ver)
    (if (str/blank? datomic-ver)
      (do (build-log/warn
           "Parameter datomic-ver is missing in build_config.edn")
          build-exit-codes/ok)
      (if-not (build-storage/setup-datomic datomic-root-dir
                                           datomic-dir-pattern
                                           datomic-url-pattern
                                           datomic-ver
                                           datomic-transactor-bin-path
                                           force)
        build-exit-codes/catch-all
        build-exit-codes/ok))))

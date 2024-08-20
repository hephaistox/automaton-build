(ns automaton-build.tasks.impl.actions.user-info
  (:require
   [automaton-build.echo.actions            :refer [errorln exceptionln]]
   [automaton-build.os.user                 :as build-user]
   [automaton-build.tasks.impl.actions.cmds :refer [blocking-cmd success]]
   [clojure.string                          :as str]))

(defn user-infos
  [prefixs verbose]
  (let [res-id (blocking-cmd prefixs
                             (build-user/user-id-cmd)
                             ""
                             "Unable to retrieve user id informations"
                             verbose)
        res-group (blocking-cmd prefixs
                                (build-user/group-id-cmd)
                                ""
                                "Unable to retrieve group id informations"
                                verbose)]
    (when (every? success [res-group res-id])
      (try (let [id (-> res-id
                        :out
                        str/trim
                        Integer/parseInt)
                 group-id (-> res-group
                              :out
                              str/trim
                              Integer/parseInt)]
             {:id id
              :group-id group-id})
           (catch Exception e
             (errorln prefixs "Unable to get user informations")
             (exceptionln prefixs e)
             nil)))))

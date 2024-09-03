(ns automaton-build.tasks.generate-code-stats
  (:require
   [automaton-build.app-data                :as build-app-data]
   [automaton-build.code-helpers.code-stats :as build-code-stats]
   [automaton-build.log                     :as build-log]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.os.files                :as build-files]
   [clojure.string                          :as str]))

(defn- generate-code-stats
  [{:keys [stats-outputfilename]
    :as app-data}]
  (build-log/info "Code statistics report")
  (let [clj-code-files (remove #(build-files/match-extension? % ".edn")
                               (build-app-data/project-paths-files app-data))
        test-files (filter #(str/includes? % "test") clj-code-files)]
    (->> (build-code-stats/clj-line-numbers clj-code-files test-files)
         (build-code-stats/stats-to-md stats-outputfilename)))
  nil)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Build all the reports"
  [_task-map app-data]
  (let [res (generate-code-stats app-data)]
    (if-not (nil? res)
      (do (build-log/error "Code stats generation has failed") build-exit-codes/catch-all)
      build-exit-codes/ok)))

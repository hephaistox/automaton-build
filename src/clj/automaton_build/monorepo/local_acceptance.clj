(ns automaton-build.monorepo.local-acceptance
  "The local acceptance for the monorepo"
  (:require
   [automaton-build.adapters.mermaid :as mermaid]
   [automaton-build.adapters.time :as time]
   [automaton-build.app.local-acceptance :as app-la]
   [automaton-build.apps.code-publication :as apps-pub]
   [automaton-build.app-agnostic.code-stats :as code-stats]
   [automaton-build.apps :as apps]
   [automaton-build.graph :as graph]
   [automaton-build.env-setup :as env-setup]))

(defn la
  "Creates a local acceptance test environment for all applictions and in that feature branch
  Params:
  * `apps` applications
  * `force?` push even if there are local modifications
  * `commit-msg` commit message"
  [apps force? commit-msg]
  (let [archi-dir (get-in env-setup/env-setup
                          [:archi :dir])]
    (mermaid/build-all-files archi-dir))

  (code-stats/stats-to-md (get-in env-setup/env-setup [:documentation :code-stats])
                          (code-stats/line-numbers "."))

  (let [graph (-> apps
                  apps/app-dependency-graph
                  apps/remove-not-required-apps)]
    (graph/topologically-ordered-doseq graph
                                       (fn [[_ {:keys [app-name]
                                                :as _node-desc}]]
                                         (apps-pub/push-a-lib (apps/search-app-by-name apps app-name)
                                                              apps
                                                              commit-msg))))

  (doseq [app apps]
    (app-la/la app
               (str "local-acceptance test:"  (time/now-str))
               force?)))

(ns automaton-build.tasks.launcher.pf-dispatcher
  (:require
   [automaton-build.tasks.launcher.launch-on-same-env
    :as
    build-launch-on-same-env]
   [automaton-build.tasks.launcher.launch-on-clj-env
    :as
    build-launch-on-clj-env]
   [automaton-build.log :as build-log]))

(defonce ^:private current-pf (atom :bb))

(defn start-clj-repl [] (reset! current-pf :clj))

(defn dispatch
  "Execute the task-fn directly in the currently running bb env or the clj env"
  [{:keys [pf task-name]
    :or {pf :bb}
    :as task-map}
   {:keys [cli-args]
    :as app-data}
   task-cli-opts]
  (build-log/info-format "Run `%s` task on pf `%s` while current-pf is: `%s`"
                         task-name
                         pf
                         @current-pf)
  (let [start-a-new-clj? (and (= :bb @current-pf) (= :clj pf))]
    (when (= :clj pf) (reset! current-pf :clj))
    (cond
      start-a-new-clj? (build-launch-on-clj-env/switch-to-clj task-map
                                                              app-data
                                                              task-cli-opts
                                                              cli-args)
      :else (build-launch-on-same-env/same-env task-map app-data))))

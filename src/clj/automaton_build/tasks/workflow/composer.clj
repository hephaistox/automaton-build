(ns automaton-build.tasks.workflow.composer
  "Compose (i.e. call successively) different tasks"
  (:require
   [automaton-build.log                 :as build-log]
   [automaton-build.os.exit-codes       :as build-exit-codes]
   [automaton-build.tasks.registry.find :as build-task-registry-find]
   [automaton-build.utils.namespace     :as build-namespace]))

(defn- execute-task-in-wf
  "In a workflow, execute one task - one step
  Returns
  * `nil` if ok
  * `exit-code` if the execution occur
  Params:
  * `task-registry`
  * `cli-opts`
  * `wk-task`"
  [task-registry app cli-opts wk-task]
  (let [{:keys [task-fn]} (build-task-registry-find/task-map task-registry
                                                             wk-task)]
    (if (nil? task-fn)
      (do
        (build-log/warn-format
         "The task `%s` has been skipped, it has not been found in the registry"
         wk-task)
        nil)
      (if-let [task-code
               (build-namespace/symbol-to-fn-call task-fn cli-opts app)]
        task-code
        build-exit-codes/fatal-error-signal))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn composer
  "Compose different tasks together
  Params:
  * `cli-opts`
  * `app`"
  [cli-opts
   {:keys [task-registry task-name]
    :as app}]
  (let [{:keys [wk-tasks]} (build-task-registry-find/task-map task-registry
                                                              task-name)]
    (loop [wk-tasks wk-tasks]
      (let [wk-task (first wk-tasks)
            rest-tasks (rest wk-tasks)
            res (execute-task-in-wf task-registry app cli-opts wk-task)]
        (cond
          (and (or (nil? res) (= res build-exit-codes/ok)) (seq rest-tasks))
          (recur rest-tasks)
          :else (cond
                  (nil? res) build-exit-codes/ok
                  (number? res) res))))))

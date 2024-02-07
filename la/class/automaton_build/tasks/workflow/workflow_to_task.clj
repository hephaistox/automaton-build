(ns automaton-build.tasks.workflow.workflow-to-task
  "Create workflow tasks, a juxt-aposition of other existing tasks.
  To do that, workflow needs:
  * A setup of workflows in `automaton-build.workflows.registry`
  * Creation of associated tasks in the bb edn, which needs to:
      * Add the tasks in the task registry
      * Creates the sumup of all cli opts for the workflow
      * Deciding if the task should be launched in bb or clj
  * A composer callable from tasks and calling successively the different tasks composing the workflow"
  (:require
   [automaton-build.tasks.registry.find :as build-task-registry-find]
   [automaton-build.log :as build-log]))

(defn- workflow-to-task
  "Transform one workflow item to a real task, with all necessary fields set"
  [task-registry
   {:keys [wk-tasks task-cli-opts-kws]
    :as workflow-item}]
  (when-not (empty? task-cli-opts-kws)
    (build-log/warn-format
     "Workflow item `%s` has cli options, it will be ignored, the ones of the wk-tasks will be used instead"
     workflow-item))
  (let [tasks-in-workflow (build-task-registry-find/task-selection task-registry
                                                                   wk-tasks)
        shared (->> (mapcat :shared tasks-in-workflow)
                    (filter some?)
                    sort
                    dedupe)
        task-cli-opts-kws (->> (mapcat :task-cli-opts-kws tasks-in-workflow)
                               (filter some?)
                               sort
                               dedupe)]
    (-> workflow-item
        (assoc :pf (if (every? #(or (nil? %) (= :bb %))
                               (mapv :pf tasks-in-workflow))
                     :bb
                     :clj)
               :shared (vec shared)
               :task-fn 'automaton-build.tasks.workflow.composer/composer
               :task-cli-opts-kws (vec task-cli-opts-kws))
        (dissoc :la-test))))

(defn update-registry-workflow-entries
  "Transform the workflow task in the registry to a classical bb-tasks registry

  Calculate which pf to run (:clj if any wk-tasks is :clj, :bb otherwise)
  And the aggregation of cli options
  Params:
  * `setuped-tasks` tasks with no setup"
  [task-registry]
  (->> task-registry
       (map (fn [[task-name
                  {:keys [wk-tasks]
                   :as workflow-item}]]
              [task-name
               (if wk-tasks
                 (workflow-to-task task-registry workflow-item)
                 workflow-item)]))))

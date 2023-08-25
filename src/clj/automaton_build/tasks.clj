(ns automaton-build.tasks
  "Tasks utility")

(def task-schema
  [:vector
   [:tuple :string [:map [:cli-params-mode keyword?]
                    [:doc :string]
                    [:exec-task fn?]]]])

(defn create-bb-tasks
  "Create bb tasks based on this
  Params:
  * `tasks` is a map associating a task name as a string to a map containing the description in the `:doc` key"
  [tasks]
  (apply sorted-map
         (mapcat (fn [[task-name {:keys [doc]}]]
                   [(symbol task-name) {:doc doc
                                        :task '(execute)}])
                 tasks)))

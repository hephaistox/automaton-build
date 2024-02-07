(ns automaton-build.la
  (:require
   [automaton-build.cli-test-runner :as build-cli-test-runner]))

(defn run
  "Defines tests to run

  Design decision:
  * This function is the center place where to set all necessary tests to be carried out in local acceptance

  Params:
  * `task-registry` registry of tasks
  * `cli-args` cli arguments to pass to all tasks to tests"
  [task-registry cli-args]
  (build-cli-test-runner/cli-test task-registry cli-args))

(ns automaton-build.monorepo.tasks.code-helpers
  "Gather monorepo to call code helpers related tasks (like stats, tests, ...)"
  (:require
   [automaton-build.adapters.cicd :as cicd]
   [automaton-core.adapters.log :as log]
   [automaton-build.adapters.outdated :as outdated]
   [automaton-build.app-agnostic.test-toolings :as tests]
   [automaton-build.apps :as apps]))

(defn gha
  "Tasks to test on github"
  [apps {:keys [force?]
         :as _task-params}]
  (if (or force?
          (cicd/is-cicd?))
    (let [code-files-repo (apps/code-files-repo apps)]
      (tests/assert-css code-files-repo)
      (tests/lint-all code-files-repo)
      (tests/alias-has-one-namespace code-files-repo)
      (tests/namespace-has-one-alias code-files-repo)
      (tests/unit-test-gha "." (cicd/is-cicd?)))
    (log/fatal "Launch only on github action, use `bb pre-push` to have more comprehensive testings, `bb gha force` to override this lock")))

(defn outdated
  "Update the code"
  [apps _task-params]
  (outdated/upgrade (map :app-dir
                         apps)))

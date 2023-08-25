(ns automaton-build.monorepo.local-test
  (:require
   [automaton-build.apps :as apps]
   [automaton-build.app.local-test :as app-ltest]
   [automaton-build.apps.templating :as app-template]
   [automaton-build.app-agnostic.test-toolings :as tests]))

(defn ltest
  "Tasks to test the code locally"
  [apps]
  (tests/unit-test ".")

  (doseq [app apps]
    (app-ltest/ltest app)
    (app-template/refresh-project app
                                  (apps/template-app apps))))

(comment

  (tests/unit-test ".")

  (app-ltest/ltest "base")

  ;;
  )

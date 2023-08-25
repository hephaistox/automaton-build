(ns automaton-build.app.test
  "Test an application"
  (:require
   [automaton-build.adapters.frontend-compiler :as frontend_compiler]
   [automaton-build.app :as app]
   [automaton-build.app-agnostic.test-toolings :as tests]))

(defn test-linters-wip
  "All kinds of linters when code is in progress
  Params:
  * `app` the app in which the code will be lintered"
  [app]
  (let [code-files-repo (app/code-files-repo app)]
    (tests/assert-css code-files-repo)
    (tests/lint-all code-files-repo)
    (tests/alias-has-one-namespace code-files-repo)
    (tests/namespace-has-one-alias code-files-repo)))

(defn test-linters-clean-code
  "All kinds of linters when code is supposed to be clean code
  Params:
  * `app` the app in which the code will be lintered"
  [app]
  (let [code-files-repo (app/code-files-repo app)]
    (tests/assert-comments code-files-repo)
    (test-linters-wip app)))

(defn test-backend
  "Test the application backend `app`
  Params:
  * `app` is where all check are done"
  [{:keys [app-dir]
    :as _app}]
  (tests/unit-test app-dir))

(defn test-frontend
  "Test the front end `app`
  Apply to automaton and cust-apps, skip otherwise
  Params:
  * `app` the app which frontend is tested"
  [{:keys [frontend? app-dir] :as _app}]
  (when frontend?
    (frontend_compiler/test-fe app-dir)))

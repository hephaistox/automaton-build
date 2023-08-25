(ns automaton-build.monorepo.tasks.publication-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.cfg-mgt :as cfg-mgt]
   [automaton-build.adapters.cfg-mgt-test :as cfg-mgt-test]
   [automaton-build.monorepo.tasks.publication :as sut]))

(def apps
  bafaaft/apps)

(def app-dir
  (files/create-temp-dir))

(def main-branch
  "main")

(def app-stub
  {:branch main-branch
   :app-dir app-dir})

(comment
  (sut/ltest apps
             {})

  (cfg-mgt/clone-repo app-dir
                      cfg-mgt-test/remote-repo-url-test
                      "main")

  (deftest new-feature-branch-test
    (testing "Create a feature branch on a fresh repo"
      (sut/new-feature-branch app-stub
                              "test-branch")
      (is (= "caumond/feature/test-branch"
             (cfg-mgt/current-branch app-dir)))
      (cfg-mgt/change-local-branch app-dir
                                   main-branch)))
  ;
  )

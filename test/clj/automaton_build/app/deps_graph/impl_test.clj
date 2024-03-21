(ns automaton-build.app.deps-graph.impl-test
  (:require
   [automaton-build.app.deps-graph.impl            :as sut]
   [automaton-build.data-structure.graph.apps-stub :as graph-apps-stub]
   [clojure.test                                   :refer
                                                   [deftest is testing]]))

(deftest app-dependency-graph-test
  (testing "Test the graph creation"
    (is
     (= {'hephaistox/app-stub {:app-dir "ldir/app_stub"
                               :app-name "app-stub"
                               :monorepo {:app-dir "app_stub"}
                               :publication {:as-lib 'hephaistox/app-stub}
                               :hephaistox-deps ['hephaistox/automaton
                                                 'hephaistox/build]
                               :deps-edn {:deps {'babashka/process {}
                                                 'hephaistox/automaton {}
                                                 'hephaistox/build {}}}}
         'hephaistox/everything {:app-dir "ldir/everything"
                                 :app-name "everything"
                                 :monorepo {:app-dir "everything"}
                                 :publication {:as-lib 'hephaistox/everything}
                                 :hephaistox-deps []
                                 :deps-edn {:deps {}}}
         'hephaistox/base-app {:app-dir "ldir/base_app"
                               :app-name "base-app"
                               :publication {:as-lib 'hephaistox/base-app}
                               :monorepo {:app-dir "base_app"}
                               :hephaistox-deps ['hephaistox/build]
                               :deps-edn {:deps {'hephaistox/build {}}}}
         'hephaistox/build {:monorepo {:app-dir "build"}
                            :app-dir "ldir/build_app"
                            :app-name "build"
                            :publication {:as-lib 'hephaistox/build}
                            :hephaistox-deps []
                            :deps-edn {:deps {}}}}
        (sut/add-hephaistox-deps graph-apps-stub/apps-w-deps-stub)))))

(deftest nodes-fn-test
  (testing "Nodes are extracted from the graph"
    (is (= #{'hephaistox/app-stub 'hephaistox/everything 'hephaistox/base-app
             'hephaistox/build}
           (->> graph-apps-stub/apps-w-deps-stub
                sut/add-hephaistox-deps
                sut/nodes-fn
                set)))))

(deftest edges-fn-test
  (testing "Edges are extracted from the graph"
    (is (= [['hephaistox/app-stub 'hephaistox/automaton]
            ['hephaistox/app-stub 'hephaistox/build]
            ['hephaistox/base-app 'hephaistox/build]]
           (->> graph-apps-stub/apps-w-deps-stub
                sut/add-hephaistox-deps
                sut/edges-fn)))))

(deftest src-in-edge-test
  (testing "Edge source"
    (is (= 'hephaistox/app-stub
           (->> graph-apps-stub/apps-w-deps-stub
                sut/add-hephaistox-deps
                sut/edges-fn
                first
                sut/src-in-edge)))))

(deftest dst-in-edge-test
  (testing "Edge dst"
    (is (= 'hephaistox/automaton
           (->> graph-apps-stub/apps-w-deps-stub
                sut/add-hephaistox-deps
                sut/edges-fn
                first
                sut/dst-in-edge)))))

(deftest remove-nodes-test
  (testing "Remove nodes"
    (is (= {}
           (-> graph-apps-stub/apps-w-deps-stub
               sut/add-hephaistox-deps
               (sut/remove-nodes #{'hephaistox/app-stub 'hephaistox/everything
                                   'hephaistox/base-app 'hephaistox/build})))))
  (testing "Remove nodes"
    (is (= {'hephaistox/app-stub {:app-dir "ldir/app_stub"
                                  :monorepo {:app-dir "app_stub"}
                                  :app-name "app-stub"
                                  :publication {:as-lib 'hephaistox/app-stub}
                                  :deps-edn {:deps {'babashka/process {}
                                                    'hephaistox/automaton {}
                                                    'hephaistox/build {}}}
                                  :hephaistox-deps ['hephaistox/automaton
                                                    'hephaistox/build]}}
           (-> graph-apps-stub/apps-w-deps-stub
               sut/add-hephaistox-deps
               (sut/remove-nodes ['hephaistox/everything
                                  'hephaistox/base-app
                                  'hephaistox/build]))))))

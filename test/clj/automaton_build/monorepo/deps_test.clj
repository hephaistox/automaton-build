(ns automaton-build.monorepo.deps-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.monorepo.deps-edn :as sut]))

(deftest build-everything-deps
  (testing "Check that everything project is found"
    (is (= {:foo :bar}
           (sut/build-everything-deps [{:deps-edn {:foo :bar}
                                        :everything? true}]))))
  (testing "`:paths` key of the project edn should be removed"
    (is (= {:foo :bar
            :aliases {:runner {:extra-paths "to be kept"}}}
           (sut/build-everything-deps [{:deps-edn {:paths "to be removed"
                                                   :aliases {:runner {:extra-paths "to be kept"}}
                                                   :foo :bar}
                                        :everything? true}]))))
  (testing "Everything project is mandatory, with its deps.edn"
    (is  (thrown-with-msg? clojure.lang.ExceptionInfo
                           #"everything project deps.edn is not found"
                           (sut/build-everything-deps [{:everything? true}])))))

(deftest build-everything-path
  (testing "Check that paths are copied, whereever they are"
    (is (= {:paths ["ev/a/" "ev/b/" "ev/c/" "ev/d/" "ev/e/" "ev/f/" "ev/g/" "app1/h/" "app1/i/" "app1/j/" "app1/k/" "app1/l/" "app1/m/"]}
           (sut/build-everything-path [{:deps-edn {:paths ["a" "b" "c"]
                                                   :aliases {:repl {:extra-paths ["d" "e"]}
                                                             :runner {:extra-paths ["f" "g"]}}}
                                        :app-dir "ev"
                                        :app-name "ev"}
                                       {:deps-edn {:paths ["h" "i"]
                                                   :aliases {:repl {:extra-paths ["j" "k"]}
                                                             :foo {:paths ["l"]}
                                                             :runner {:extra-paths ["m"]}}}
                                        :app-name "app1"
                                        :app-dir "app1"}])))))

(deftest build-deps
  (testing "Able to gather deps from apps, whereever there are"
    (is (= {:deps {'org.clojure/clojure {:mvn/version "1.11.1"}
                   'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}
                   'foo/bar {:mvn/version "1.1.0"}
                   'bar/foo {:mvn/version "1.1.0"}}}
           (sut/build-deps [{:deps-edn {:deps {'org.clojure/clojure {:mvn/version "1.11.1"}}}}
                            {:deps-edn {:deps {'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}}
                                        :aliases {:repl {:extra-deps {'foo/bar {:mvn/version "1.1.0"}}}
                                                  :runner {:extra-deps {'bar/foo {:mvn/version "1.1.0"}}}}}}

                            "base-app" {:publication {:as-lib 'hephaistox.automaton}}]))))
  (testing "Base project is removed"
    (is (= {:deps {'org.clojure/clojure {:mvn/version "1.11.1"}
                   'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}
                   'hephaistox/automaton {:local/root "app1"}}}
           (sut/build-deps [{:deps-edn {:deps {'org.clojure/clojure {:mvn/version "1.11.1"}}}}
                            {:deps-edn {:deps {'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}
                                               'hephaistox/automaton {:local/root "app1"}}}}])))
    (is (= {:deps {'org.clojure/clojure {:mvn/version "1.11.1"}
                   'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}
                   'hephaistox/automaton {:local/root "app1"}}}
           (sut/build-deps [{:deps-edn {:deps {'org.clojure/clojure {:mvn/version "1.11.1"}}}}
                            {:deps-edn {:deps {'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}
                                               'hephaistox/automaton {:local/root "app1"}}}}])))))

(deftest build-deps-edn
  (testing "Generate expected `deps.edn`, test a whole `deps.edn` generation"
    (is (= {:foo :bar
            :deps {'org.clojure/clojure {:mvn/version "1.11.1"}
                   'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}}
            :paths ["everything/src1/" "build/a/" "app1/src4/" "app1/src6/"]
            :aliases {:build {:paths ["build/a/"]
                              :extra-paths ["build/b/" "build/c/"]}}}
           (sut/create-build-deps-edn [{:deps-edn {:foo :bar
                                                   :deps {'org.clojure/clojure {:mvn/version "1.11.1"}}
                                                   :paths ["src1"]
                                                   :aliases {}}
                                        :app-name "everything"
                                        :app-dir "everything"
                                        :everything? true}
                                       {:deps-edn {:paths ["a"]
                                                   :extra-paths ["b" "c"]}
                                        :build? true
                                        :app-dir "build"
                                        :app-name "build"}
                                       {:deps-edn {:foo1 :bar1
                                                   :deps {'com.clojure-goes-fast/clj-memory-meter {:mvn/version "0.2.1"}}
                                                   :paths ["src4"]
                                                   :aliases {:runner {:extra-paths ["src6"]}}}
                                        :app-dir "app1"
                                        :app-name "app1"}
                                       {:app-name "base-app"
                                        :app-dir "base_app"
                                        :publication {:as-lib 'hephaistox/automaton}}])))))


(ns automaton-build.app.deps-edn-test
  (:require
   [automaton-build.app.deps-edn :as sut]
   [automaton-build.os.files     :as build-files]
   [clojure.test                 :refer [deftest is testing]]))

(def tmp-dir (build-files/create-temp-dir))

(deftest get-deps-filename-test
  (testing "Get deps is working" (is (string? (sut/deps-path tmp-dir)))))

(deftest extract-paths-test
  (testing "Extract paths from a deps.edn file"
    (is (= ["a" "b" "c" "d" "e" "f" "g"]
           (sut/extract-paths {:paths ["a" "b" "c"]
                               :aliases {:repl {:extra-paths ["d" "e"]}
                                         :runner {:extra-paths ["f" "g"]}}}
                              #{})))
    (is (= ["a" "b" "c" "d" "e" "f" "g"]
           (sut/extract-paths {:paths ["a" "b" "c"]
                               :aliases {:repl {:extra-paths ["d" "e"]}
                                         :runner {:extra-paths ["f" "g"]}}}
                              #{}))))
  (testing "Exclusion of aliases is working"
    (is (= ["a" "b" "c" "d" "e"]
           (sut/extract-paths {:paths ["a" "b" "c"]
                               :aliases {:repl {:extra-paths ["d" "e"]}
                                         :runner {:extra-paths ["f" "g"]}}}
                              #{:runner})))
    (is (= ["a" "b" "c" "f" "g"]
           (sut/extract-paths {:paths ["a" "b" "c"]
                               :aliases {:repl {:extra-paths ["d" "e"]}
                                         :runner {:extra-paths ["f" "g"]}}}
                              #{:repl}))))
  (testing "Dedupe works"
    (is (= ["a" "b" "c" "f" "g"]
           (sut/extract-paths {:paths ["f" "g"]
                               :aliases {:repl {:extra-paths ["d" "e" "f"]}
                                         :runner {:extra-paths ["a" "b" "c" "f"]}}}
                              #{:repl})))))

(deftest load-deps-edn-test
  (testing "Existing file is found" (is (map? (sut/slurp ""))))
  (testing "Non existing file is ok" (is (nil? (sut/slurp "non-existing-dir")))))

(deftest is-hephaistox-deps-test
  (testing "Hephaistox lib find"
    (is (sut/is-hephaistox-deps ['hephaistox/automaton-build {:mvn/version ""}])))
  (testing "Non hephaistox lib is skipped"
    (is (not (sut/is-hephaistox-deps [:is-a-dep {:mvn/version ""}])))))

(deftest hephaistox-deps-test
  (testing "Hephaistox deps are selected"
    (is (= ['hephaistox/automaton-build]
           (sut/hephaistox-deps {:deps {'hephaistox/automaton-build {}
                                        'clojure.core {}}})))))

(deftest compare-deps-test
  (testing "First one is lower"
    (is (= {:mvn/version "0.1.3"} (sut/compare-deps {:mvn/version "0.1.2"} {:mvn/version "0.1.3"})))
    (is (= {:mvn/version "0.2.0"} (sut/compare-deps {:mvn/version "0.1.2"} {:mvn/version "0.2.0"})))
    (is (= {:mvn/version "1.0.0"}
           (sut/compare-deps {:mvn/version "0.1.2"} {:mvn/version "1.0.0"}))))
  (testing "Are identical"
    (is (= {:mvn/version "0.1.2"}
           (sut/compare-deps {:mvn/version "0.1.2"} {:mvn/version "0.1.2"}))))
  (testing "Second one is lower"
    (is (= {:mvn/version "0.1.4"}
           (sut/compare-deps {:mvn/version "0.1.4"} {:mvn/version "0.1.3"})))))

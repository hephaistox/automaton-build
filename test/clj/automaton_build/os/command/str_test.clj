(ns automaton-build.os.command.str-test
  (:require
   [automaton-build.os.command.str :as sut]
   [babashka.process :as babashka-process]
   [clojure.test :refer [deftest is testing]]))

(deftest cmd-tokens-test
  (testing "cmd-tokens"
    (is (= ["ls"] (sut/cmd-tokens ["ls"])))
    (is (empty? (sut/cmd-tokens [])))
    (is (empty? (sut/cmd-tokens nil)))))

(deftest opts-test
  (testing "opts"
    (is (= {:in :inherit
            :out :inherit
            :dir "."
            :shutdown babashka-process/destroy-tree}
           (sut/opts ["ls"])))))

(deftest add-opts-test
  (testing "add-opts"
    (is (= ["ls"
            "az"
            {:in :inherit
             :out :inherit
             :shutdown babashka.process/destroy-tree
             :dir "."
             :foo :bar}]
           (sut/add-opts ["ls" "az"] {:foo :bar})))
    (is (= ["ls"
            {:in :inherit
             :out :inherit
             :shutdown babashka.process/destroy-tree
             :dir "."
             :foo :bar}]
           (sut/add-opts ["ls"] {:foo :bar})))
    (is (= ["ls"
            {:in :foo
             :out :inherit
             :shutdown babashka.process/destroy-tree
             :dir "."}]
           (sut/add-opts ["ls"] {:in :foo})))))

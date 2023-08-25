(ns automaton-build.adapters.commands-test
  "These tests are more or less always true (fail if an exception).
Their value are to catch knowledge on how to use commands.
And be able to watch bb log to check if results are as expected"
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.commands :as sut]
   [automaton-build.adapters.schema :as schema]))

(defn check-exit-code
  "Check the process return code is 0"
  [proc]
  (= 0 (:exit proc)))

(deftest execute-command-test
  (testing "Simple command is ok"
    (is
     (check-exit-code
      (sut/execute-command [["ls" "-la"] {}]
                           {:dir "."
                            :out :string}))))
  (testing "Output is caught in :string"
    (is
     (= "-la\n"
        (:out
         (sut/execute-command [["echo" "-la"] {}]
                              {:out :string
                               :dir "."})))))
  (testing "Command exits with non zero return code"
    (is
     (thrown-with-msg? clojure.lang.ExceptionInfo
                       #"failed on exit code"
                       (sut/execute-command [["uname" "-x"] {}]
                                            {:out :string
                                             :dir "."}))))
  (testing "Directory does not exist"
    (is
     (thrown-with-msg? clojure.lang.ExceptionInfo
                       #"Command.*failed"
                       (sut/execute-command [[] {}]
                                            {:out :string
                                             :dir "."}))))
  (testing "Directory is already a file"
    (is
     (thrown-with-msg? clojure.lang.ExceptionInfo
                       #"Can't create a directory"
                       (sut/execute-command [["pwd"]]
                                            {:dir "deps.edn"
                                             :out :string})))))

(deftest execute-test
  (testing "Simple version"
    (is
     (string?
      (sut/exec-cmds [[["pwd"]]
                      [["pwd"]]]
                     {:out :string
                      :dir "."}))))
  (testing "Failed command is detected"
    (is
     (thrown-with-msg? clojure.lang.ExceptionInfo
                       #""
                       (sut/exec-cmds [[["uname" "-x"]
                                        {:out :string}]]))))
  (testing "Commands should be a vector of command"
    (is
     (thrown-with-msg? clojure.lang.ExceptionInfo
                       #"Malformed command"
                       (sut/exec-cmds "test")))))

(deftest commands-schema-test
  (testing "Accepted example"
    (is (schema/schema-valid sut/commands-schema
                             [[[]]]))
    (is (schema/schema-valid sut/commands-schema
                             [[["pwd"]]]))
    (is (schema/schema-valid sut/commands-schema
                             [[["pwd"]]
                              [["pwd"]]])))
  (testing "Non accepted example"
    (is (not (schema/schema-valid sut/commands-schema
                                  [[["pwd" nil]]])))
    (is (not (schema/schema-valid sut/commands-schema
                                  [[["pwd" nil] {} {}]])))
    (is (not (schema/schema-valid sut/commands-schema
                                  [[["pwd" nil] [] {}]])))))

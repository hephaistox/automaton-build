(ns automaton-build.os.cmds-test
  (:require
   [automaton-build.os.cmds     :as sut]
   [automaton-build.os.filename :as build-filename]
   [clojure.test                :refer [deftest is]]))

(deftest to-str-test (is (= "ls -la" (sut/to-str ["ls" "-la"]))))

(deftest clj-parameterize-test
  (is (= "'\"foo\"'" (sut/clj-parameterize "foo")))
  (is (= 3 (sut/clj-parameterize 3))))


(deftest blocking-cmd-str-test
  (is (= {:cmd-str ""
          :out nil
          :exit nil
          :err nil}
         (dissoc (sut/blocking-cmd-str "" ".") :dir))
      "An empty command is skipped.")
  (is (= {:cmd-str "echo so"
          :out "so\n"
          :exit 0
          :err ""}
         (dissoc (sut/blocking-cmd ["echo" "so"] ".") :dir))
      "A valid command is executed and return message.")
  (is (= {:cmd-str "ls non-existing-dir"
          :out ""
          :exit 1
          :err "ls: non-existing-dir: No such file or directory\n"}
         (dissoc (sut/blocking-cmd ["ls" "non-existing-dir"] ".") :dir))
      "A failing coding has a non zero exit code, and display messages in error.")
  (is (= {:cmd-str "non-existing-command"
          :exit -1}
         (-> (sut/blocking-cmd ["non-existing-command"] "")
             (dissoc :dir :e :err)))
      "An invalid command raises an exception"))

(deftest force-dirs
  (is (= [[["ls"] "target-dir"] [["pwd"] "target-dir"]]
         (sut/force-dirs [[["ls"]] [["pwd"] "dir/to/remove"]] "target-dir"))))

(deftest chain-cmds-test
  (is (= (let [current-dir (build-filename/absolutize ".")
               c-dir (build-filename/absolutize "")]
           [{:dir current-dir
             :cmd ["pwd"]
             :cmd-str "pwd"
             :out (str c-dir "\n")
             :exit 0
             :err ""}
            {:dir current-dir
             :cmd ["pwd"]
             :cmd-str "pwd"
             :out (str c-dir "\n")
             :exit 0
             :err ""}
            {:dir current-dir
             :cmd ["echo" "hi!"]
             :cmd-str "echo hi!"
             :out "hi!\n"
             :exit 0
             :err ""}])
         (-> [[["pwd"]] [["pwd"]] [["echo" "hi!"]]]
             (sut/force-dirs ".")
             sut/chain-cmds))
      "All commands are executed, only last result is returned.")
  (is (-> [[["pwd"]] [["pwd"]] [["non-existing-cmd"]]]
          (sut/force-dirs ".")
          sut/chain-cmds
          (get 2)
          :e)
      "Last failing is caught and the exception returned.")
  (is (not (zero? (-> [[["pwd"]] [["ls" "non-existing file"]] [["pwd"]]]
                      (sut/force-dirs ".")
                      sut/chain-cmds
                      (get 1)
                      :exit)))
      "First failing command is stopping, returns its exit code.")
  (is (= [0 -1 nil]
         (->> [[["ls"] "."] [["non-existing-cmd"] "."] [["echo" "hi!"] "."]]
              sut/chain-cmds
              (mapv :exit)))
      "Returns the first failing command"))

(deftest first-failing-test
  (is (= ["non-existing-cmd"]
         (-> [[["ls"] "."] [["non-existing-cmd"] "."] [["echo" "hi!"] "."]]
             sut/chain-cmds
             sut/first-failing
             :cmd))
      "Returns the first failing command"))

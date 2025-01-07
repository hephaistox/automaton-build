(ns automaton-build.os.cmd-test
  (:require
   [automaton-build.os.cmd :as sut]
   [clojure.test           :refer [deftest is]]))

;; ********************************************************************************
;; Build the command
;; ********************************************************************************

(deftest to-str-test (is (= "ls -la" (sut/to-str ["ls" "-la"]))))

(deftest absolutize-test
  (is (= (sut/absolutize nil) (sut/absolutize "") (sut/absolutize "."))
      "`nil`, emtpy string and . should all return the same value"))

;; ********************************************************************************
;; Blocking until the end of the command
;; ********************************************************************************

(deftest blocking-cmd-str-test
  (is (= {:cmd ""
          :out nil
          :dir "."
          :exit nil
          :err nil}
         (-> ""
             (sut/blocking-cmd ".")
             (dissoc :adir)))
      "An empty command is skipped.")
  (is (= {:cmd ["echo" "so"]
          :out "so\n"
          :dir ""
          :exit 0
          :err ""}
         (-> ["echo" "so"]
             (sut/blocking-cmd "")
             (dissoc :adir)))
      "A valid command is executed and return message.")
  (is (= {:cmd ["ls" "non-existing-dir"]
          :out ""
          :dir "."
          :exit 1
          :err "ls: non-existing-dir: No such file or directory\n"}
         (-> ["ls" "non-existing-dir"]
             (sut/blocking-cmd ".")
             (dissoc :adir)))
      "A failing coding has a non zero exit code, and display messages in error.")
  (is (= {:cmd ["non-existing-command"]
          :dir ""
          :exit -1}
         (-> ["non-existing-command"]
             (sut/blocking-cmd "")
             (dissoc :adir :exception :err)))
      "An invalid command raises an exception"))

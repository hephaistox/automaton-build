(ns automaton-build.tasks.impl.actions.cmds-test
  (:require
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.tasks.impl.actions.cmds :as sut]
   [clojure.string                          :as str]
   [clojure.test                            :refer [deftest is]]))

;; Simple echoing delegation needs no test

;;  Printing command
(deftest print-cmd-str-test
  (is (= "f-g>`pwd`\n[39m" (with-out-str (sut/print-cmd-str ["f" "g"] "pwd")))))

(deftest print-exec-cmd-str-test
  (is (= "f-g>exec on bash: `pwd`\n[39mf-g>(in directory: `.` )\n[39m"
         (with-out-str (sut/print-exec-cmd-str ["f" "g"] "pwd" ".")))))

;; Execute commands
(deftest blocking-cmd-test
  (is
   (=
    "[31mp-q>Demonstrates an execution error.\n[39mp-q>This command has failed:\n[39mp-q>`non-existing-cmd`\n[39mp-q>\n[39mp-q>Cannot run program \"non-existing-cmd\" : error=2, No such file or directory\n[39m"
    (-> (with-out-str (:exit (sut/blocking-cmd ["p" "q"]
                                               ["non-existing-cmd"]
                                               ""
                                               "Demonstrates an execution error."
                                               false)))
        (str/replace #"\(in directory .*\)" "")))
   "Demonstrates an execution error.")
  (is (= "p-q>exec on bash: `echo 3`\n[39mp-q>(in directory:"
         (subs (with-out-str
                 (dissoc (sut/blocking-cmd ["p" "q"] ["echo" "3"] "" "Should not be displayed" true)
                  :dir))
               0
               50))))


(deftest long-living-cmd-test
  (is (= ""
         (with-out-str (sut/long-living-cmd ["p" "q"]
                                            ["non-existing" "3"]
                                            ""
                                            1
                                            false
                                            (constantly true)
                                            (constantly true))))
      "Failing command doesn't display anything.")
  (is (= {:dir (build-filename/absolutize ".")
          :cmd-str "non-existing 3"}
         (dissoc (sut/long-living-cmd ["p" "q"]
                                      ["non-existing" "3"]
                                      ""
                                      1
                                      false
                                      (constantly true)
                                      (constantly true))
          :e))
      "Failing command returns dir cmd-str and e, doesn't return proc.")
  (is (str/blank? (with-out-str (sut/long-living-cmd ["p" "q"]
                                                     ["sleep" "0.001"]
                                                     ""
                                                     1
                                                     false
                                                     (constantly true)
                                                     (constantly true))))
      "A sucessful non verbose command is quiet.")
  (is (= "p-q>exec on bash: `sleep 0.001`\n[39mp-q>(in directory:"
         (subs (with-out-str (sut/long-living-cmd ["p" "q"]
                                                  ["sleep" "0.001"]
                                                  ""
                                                  1
                                                  true
                                                  (constantly true)
                                                  (constantly true)))
               0
               55))
      "A verbose sucessful command displays its execution.")
  (is
   (=
    "p-q>3\n[39m"
    (with-out-str
      (sut/long-living-cmd ["p" "q"] ["echo" "3"] "" 1 false (constantly true) (constantly true))))
   "Output are caught."))

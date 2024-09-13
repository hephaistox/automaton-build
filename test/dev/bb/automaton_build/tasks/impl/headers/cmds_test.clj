(ns automaton-build.tasks.impl.headers.cmds-test
  (:require
   [automaton-build.echo.headers            :as build-echo-headers]
   [automaton-build.tasks.impl.headers.cmds :as sut]
   [clojure.string                          :as str]
   [clojure.test                            :refer [deftest is]]))

;; Simple echoing delegation needs no test

;;  Printing command
(deftest print-cmd-str-test
  (is (= "[39m    `pwd`\n[39m"
         (do (with-out-str (build-echo-headers/h2 "tst"))
             (with-out-str (sut/print-cmd-str "pwd"))))))

(deftest print-exec-cmd-str-test
  (is (= "[39m    exec on bash: `pwd`\n[39m[39m    (in directory: `.` )\n[39m"
         (do (with-out-str (build-echo-headers/h2 "tst"))
             (with-out-str (sut/print-exec-cmd-str "pwd" "."))))))

;; Execute commands
(deftest blocking-cmd-test
  (with-out-str (build-echo-headers/h2 "test"))
  (is (not= 0
            (:exit
             (sut/blocking-cmd ["non-existing-cmd"] "" "Demonstrates an execution error." false)))
      "Fails with an error code")
  (is (= "[39m    exec on bash: `echo 3`\n[39m[39m    \n[39m"
         (-> (with-out-str (dissoc (sut/blocking-cmd ["echo" "3"] "" "Should not be displayed" true)
                            :dir))
             (str/replace #"\(in directory: .*\)" "")))))

(deftest long-living-cmd-test
  (is (=
       ""
       (with-out-str
         (sut/long-living-cmd ["non-existing" "3"] "" 1 false (constantly true) (constantly true))))
      "Failing command doesn't display anything.")
  (is (= {:cmd-str "non-existing 3"}
         (->
           (sut/long-living-cmd ["non-existing" "3"] "" 1 false (constantly true) (constantly true))
           (dissoc :e :dir)))
      "Failing command returns dir cmd-str and e, doesn't return proc.")
  (is (str/blank?
       (with-out-str
         (sut/long-living-cmd ["sleep" "0.001"] "" 1 false (constantly true) (constantly true))))
      "A sucessful non verbose command is quiet.")
  (is (= "[39m    exec on bash: `sleep 0.001`\n[39m[39m    (in directory:"
         (->
           (with-out-str
             (sut/long-living-cmd ["sleep" "0.001"] "" 1 true (constantly true) (constantly true)))
           (subs 0 65)))
      "A verbose sucessful command displays its execution.")
  (is (= "[39m    3\n[39m"
         (with-out-str
           (sut/long-living-cmd ["echo" "3"] "" 1 false (constantly true) (constantly true))))
      "Output are caught."))

(deftest chain-cmds-test
  (is (-> [[["ls"]] [["ls"]]]
          (sut/force-dirs ".")
          (sut/chain-cmds "Should not be displayed." false))))

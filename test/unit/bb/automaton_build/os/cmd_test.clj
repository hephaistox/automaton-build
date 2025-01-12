(ns automaton-build.os.cmd-test
  (:require
   [automaton-build.echo.common :refer [build-writter]]
   [automaton-build.os.cmd      :as sut]
   [clojure.test                :refer [deftest is testing]]))

(deftest create-process-test
  (testing "Test the timing"
    (is
     (> 20
        (->> (sut/create-process ["sleep" "3"] "" nil nil nil 1 nil 0 0)
             time
             with-out-str
             (re-find #"\d+\.\d+")
             Double/parseDouble))
     "As it is non blocking, `create-process` should be instantaneous, even if the command is long")
    (is (> 1300
           (->> (sut/create-process ["sleep" "1"] "" nil nil nil 1 nil 0 0)
                time
                with-out-str
                (re-find #"\d+\.\d+")
                Double/parseDouble))
        "wait-for is waiting for the end of the process, so more than a second"))
  (testing "what is printed"
    (is (= ["workflows\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["ls"]
                   (sut/create-process ".github" println nil nil 1 nil 0 0))
               ;; This wait leave time for the messages to be caught in *out* and *err*
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "A successfull command printing its out stream only")
    (is (= ["workflows\nend of command\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["ls"]
                   (sut/create-process ".github" println nil #(println "end of command") 1 nil 0 0))
               ;; This wait leave time for the messages to be caught in *out* and *err*
               (Thread/sleep 100)
               (->> [out err]
                    (mapv str)))))
        "A successfull command printing its out and the end of the command")
    (is (= ["ls: non-existing: No such file or directory\nend of command\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["ls" "non-existing"]
                   (sut/create-process ".github"
                                       println
                                       println
                                       #(println "end of command")
                                       1 #(println "can't start command " %)
                                       0 0))
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "A command failure")
    (is (= ["can't start command  [non-existing]\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["non-existing"]
                   (sut/create-process ".github"
                                       println
                                       println
                                       #(println "end of command")
                                       1 #(println "can't start command " %)
                                       0 0))
               (->> [out err]
                    (mapv str)))))
        "A non existing command which is not starting")))

(deftest still-running?-test
  (is (-> ["sleep" "1"]
          (sut/muted-non-blocking "")
          sut/still-running?)
      "For an in progress command, still-running? returns `true`")
  (is (not (let [p (-> (sut/muted-non-blocking ["echo" "2"] "")
                       (sut/wait-for nil nil))]
             (Thread/sleep 10)
             (sut/still-running? p)))
      "For a succesfull command still-running? returns `true`")
  (is (not (let [p (-> (sut/muted-non-blocking ["non-existing"] "")
                       (sut/wait-for nil nil))]
             (Thread/sleep 10)
             (sut/still-running? p)))
      "For a failed command, still-running? returns `true`"))

(deftest wait-for-test
  (testing "Returned values"
    (is (= {:status :didnt-start}
           (-> ["non existing"]
               (sut/muted-non-blocking "")
               (sut/wait-for nil nil)
               (select-keys [:status])))
        "A non existing command is noop")
    (is (= {:status :failure
            :out-stream []
            :err-stream []}
           (-> ["ls" "non-existing"]
               (sut/muted-non-blocking "")
               (sut/wait-for nil nil)
               (select-keys [:status :out-stream :err-stream])))
        "A failing command")
    (is (= {:status :success
            :out-stream []
            :err-stream []}
           (-> ["ls"]
               (sut/muted "")
               (sut/wait-for println println)
               (select-keys [:status :out-stream :err-stream])))
        "A successfull command"))
  (testing "What is printed by wait-for"
    (is (= ["" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["non-existing"]
                   (sut/muted-non-blocking "")
                   (sut/wait-for println println))
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "wait-for is noop for a not started command")
    (is (= ["Error during execution of `ls non-existing`\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["ls" "non-existing"]
                   (sut/muted-non-blocking "")
                   (sut/wait-for println println))
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Wait-for is printing the error message for a failed commands")
    (is (= ["" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (-> ["ls"]
                   (sut/muted-non-blocking ".github")
                   (sut/wait-for println println))
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Successful command prints nothing")))

(deftest kill-test
  (is (-> (sut/muted-non-blocking ["sleep" "20"] "")
          (sut/kill nil)
          :killed?)
      "A long op could be killed"))

;; ********************************************************************************
;; High level API
;; ********************************************************************************

(deftest muted-test
  (testing "What it prints -> nothing !"
    (is (= ""
           (with-out-str (-> ["ls"]
                             (sut/muted ".github"))))
        "A successfull command")
    (is (= ""
           (with-out-str (-> ["non existing"]
                             (sut/muted ""))))
        "A non existing command which is not starting")
    (is (= ""
           (with-out-str (-> ["ls" "non existing"]
                             (sut/muted ""))))
        "A failing command"))
  (testing "Returned data"
    (is (= {:cmd ["ls"]
            :cmd-str "ls"
            :dir ".github"
            :status :success
            :adir true
            :out-stream true
            :err-stream true
            :bb-proc true}
           (-> ["ls"]
               (sut/muted ".github")
               (update :bb-proc map?)
               (update :adir string?)
               (update :out-stream some?)
               (update :err-stream some?)))
        "A successfull command")
    (is (= {:status :didnt-start
            :exception true}
           (-> ["non-existing"]
               (sut/muted "")
               (select-keys [:status :exception])
               (update :exception some?)))
        "A non existing command which is not starting")
    (is (= {:status :failure}
           (-> ["ls" "non-existing"]
               (sut/muted "")
               (select-keys [:status :exception])))
        "A non existing command which is not starting")))

(deftest muted-non-blocking-test
  (testing "What it prints -> nothing !"
    (is (= ""
           (with-out-str (-> ["ls"]
                             (sut/muted-non-blocking ".github"))))
        "A successfull command")
    (is (= ""
           (with-out-str (-> ["non existing"]
                             (sut/muted-non-blocking ""))))
        "A non existing command which is not starting")
    (is (= ""
           (with-out-str (-> ["ls" "non existing"]
                             (sut/muted-non-blocking ""))))
        "A failing command"))
  (testing "Returned data"
    (is (= {:cmd ["echo" "2"]
            :cmd-str "echo 2"
            :dir ""
            :status :success
            :adir true
            :bb-proc true
            :out-stream []
            :err-stream []}
           (let [p (-> (sut/muted-non-blocking ["echo" "2"] "")
                       (sut/wait-for nil nil))]
             (Thread/sleep 10)
             (-> p
                 (update :adir string?)
                 (update :bb-proc map?))))
        "A succesfully finished command is return success")
    (is (= {:out-stream []
            :err-stream []
            :status :failure}
           (let [p (-> (sut/muted-non-blocking ["ls" "non-existing"] "")
                       (sut/wait-for nil nil))]
             (Thread/sleep 10)
             (-> p
                 (select-keys [:out-stream :err-stream :status]))))
        "A failing command")
    (is (= {:status :didnt-start}
           (let [p (-> (sut/muted-non-blocking ["non-existing"] "")
                       (sut/wait-for nil nil))]
             (Thread/sleep 10)
             (-> p
                 (select-keys [:out-stream :err-stream :status]))))
        "A non starting command")))

(deftest as-string
  (testing "Check returned data"
    (is (= {:dir ".github"
            :status :success
            :bb-proc true
            :adir true
            :cmd ["ls"]
            :cmd-str "ls"
            :out-stream ["workflows"]
            :err-stream []}
           (-> (sut/as-string ["ls"] ".github")
               (update :adir string?)
               (update :bb-proc map?)))
        "Returned data of a succesful command")
    (is (= {:status :didnt-start
            :exception true}
           (-> (sut/as-string ["non-existing-cmd"] "")
               (select-keys [:status :exception])
               (update :exception some?)))
        "Returned data of a non starting command")
    (is (= {:status :failure
            :out-stream []
            :err-stream ["ls: non-existing-cmd: No such file or directory"]}
           (-> (sut/as-string ["ls" "non-existing-cmd"] "")
               (select-keys [:status :out-stream :err-stream])))
        "Returned data of a failing command"))
  (testing "Print nothing"
    (is (= ["" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/as-string ["ls"] "")
               (->> [out err]
                    (mapv str)))))
        "Succesfull command")
    (is (= ["" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/as-string ["non-existing-cmd"] "")
               (->> [out err]
                    (mapv str)))))
        "Non starting command")
    (is (= ["" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/as-string ["ls" "non-existing-cmd"] "")
               (->> [out err]
                    (mapv str)))))
        "Failing command")))

(deftest printing-test
  (testing "Print everything"
    (is (= ["out: workflows\nend of command\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/printing ["ls"]
                             ".github"
                             (partial println "out:")
                             (partial println "err:")
                             (partial println "end of command")
                             1)
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Succesfull command")
    (is (= ["err: Cant' start [non-existing-cmd]\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/printing ["non-existing-cmd"]
                             ".github"
                             (partial println "out:")
                             (partial println "err:")
                             (partial println "end of command")
                             1)
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Non starting command")
    (is (= ["err: ls: non-existing-cmd: No such file or directory\nend of command\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/printing ["ls" "non-existing-cmd"]
                             ""
                             (partial println "out:")
                             (partial println "err:")
                             (partial println "end of command")
                             1)
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Failing command")))

(deftest printing-blocking-test
  (testing "Print everything"
    (is (= ["out: workflows\nend of command\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/printing ["ls"]
                             ".github"
                             (partial println "out:")
                             (partial println "err:")
                             (partial println "end of command")
                             1)
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Succesfull command")
    (is (= ["err: Cant' start [non-existing-cmd]\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/printing ["non-existing-cmd"]
                             ".github"
                             (partial println "out:")
                             (partial println "err:")
                             (partial println "end of command")
                             1)
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Non starting command")
    (is (= ["err: ls: non-existing-cmd: No such file or directory\nend of command\n" ""]
           (let [out (build-writter)
                 err (build-writter)]
             (binding [*err* err
                       *out* out]
               (sut/printing ["ls" "non-existing-cmd"]
                             ""
                             (partial println "out:")
                             (partial println "err:")
                             (partial println "end of command")
                             1)
               (Thread/sleep 10)
               (->> [out err]
                    (mapv str)))))
        "Failing command")))

(deftest printing-only-error-test
  (is (= ["" ""]
         (let [out (build-writter)
               err (build-writter)]
           (binding [*err* err
                     *out* out]
             (sut/print-on-error ["ls"]
                                 ".github"
                                 (partial println "out:")
                                 (partial println "err:")
                                 1
                                 10
                                 10)
             (Thread/sleep 10)
             (->> [out err]
                  (mapv str)))))
      "Succesfull command prints nothing")
  (is (= ["err: Cant' start [non-existing-cmd]\n" ""]
         (let [out (build-writter)
               err (build-writter)]
           (binding [*err* err
                     *out* out]
             (sut/print-on-error ["non-existing-cmd"]
                                 ".github"
                                 (partial println "out:")
                                 (partial println "err:")
                                 1
                                 10
                                 10)
             (Thread/sleep 10)
             (->> [out err]
                  (mapv str)))))
      "Non starting command")
  (is
   (=
    ["err: Error during execution of `ls non-existing-cmd`\nout: Error stream:\nout: ls: non-existing-cmd: No such file or directory\n"
     ""]
    (let [out (build-writter)
          err (build-writter)]
      (binding [*err* err
                *out* out]
        (sut/print-on-error ["ls" "non-existing-cmd"]
                            ""
                            (partial println "out:")
                            (partial println "err:")
                            1
                            10
                            10)
        (Thread/sleep 10)
        (->> [out err]
             (mapv str)))))
   "Failing command"))

(ns automaton-build.os.cmd-test
  (:require
   [automaton-build.os.cmd :as sut]
   [clojure.test           :refer [deftest is]]))

(deftest non-blocking-test
  (is (= :wip
         (-> ["sleep" "3"]
             (sut/non-blocking "" nil nil nil 100)
             :status))
      "The status starts with `wip`")
  (is (= :didnt-start
         (-> ["non-existing" "3"]
             (sut/non-blocking "" nil nil nil 100)
             :status))
      "The status starts with `wip`")
  (is (> 15
         (->> (sut/non-blocking ["sleep" "3"] "" nil nil nil 100)
              time
              with-out-str
              (re-find #"\d+\.\d+")
              Double/parseDouble))
      "As it is non blocking, `non-blocking` should be instantaneous, even if the command is long")
  (is (> 1300
         (->>
           (sut/non-blocking ["sleep" "1"] "" #(println "out:" %) #(println "out finished") nil 100)
           time
           with-out-str
           (re-find #"\d+\.\d+")
           Double/parseDouble))
      "wait-for is waiting for the end of the process")
  (is (= "workflows\n"
         (with-out-str (do (sut/non-blocking ["ls"] ".github" println println nil 100)
                           (Thread/sleep 100))))
      "Outputs are caught"))

(deftest still-running?-test
  (is (-> ["sleep" "2"]
          (sut/non-blocking "" nil nil nil 100)
          sut/still-running?)
      "When not finished, a long op is still running?")
  (is (not (let [p (sut/non-blocking ["echo" "2"] "" nil nil nil 100)]
             (Thread/sleep 100)
             (sut/still-running? p)))
      "After a command has stopped, still-running? returns false"))

(deftest kill-test
  (is (-> (sut/non-blocking ["sleep" "20"] "" nil nil nil 100)
          sut/kill
          :killed?)
      "A long op could be killed"))

;; ********************************************************************************
;; Blocking until the end of the command
;; ********************************************************************************

(deftest returning-str-test
  (is (= #{:cmd :dir :out-param :err-param :adir :exception :status :out :err}
         (-> (sut/returning-str ["aze"] ".github")
             keys
             set)))
  (is
   (= {:cmd ["ls"]
       :dir ".github"
       :out-param :string
       :err-param :string
       :err ""
       :out "workflows\n"
       :status :success}
      (-> (sut/returning-str ["ls"] ".github")
          (dissoc :bb-proc :adir)))
   "The .github directory contains only workflows, the `return-str` function fetch in `out` the output in the string format")
  (is (= {:cmd ["ls" "non-existing-file"]
          :dir ""
          :out-param :string
          :err-param :string
          :err true
          :out ""
          :status :failure}
         (-> (sut/returning-str ["ls" "non-existing-file"] "")
             (dissoc :bb-proc :adir)
             (update :err string?)))
      "A command which execution has failed is returning a failure status")
  (is (= {:dir ".github"
          :err nil
          :err-param :string
          :out nil
          :status :didnt-start
          :cmd ["lzaels"]
          :out-param :string}
         (-> (sut/returning-str ["lzaels"] ".github")
             (dissoc :proc :exception :adir)))
      "A non existing command"))

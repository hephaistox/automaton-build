(ns automaton-build.cicd.cfg-mgt-test
  (:require
   [automaton-build.cicd.cfg-mgt :as sut]
   [automaton-build.cicd.server :as build-cicd-server]
   [clojure.test :refer [deftest is testing]]
   [automaton-build.os.files :as build-files]
   [clojure.string :as str]))

(deftest git-installed?*-test
  (when-not (build-cicd-server/is-cicd?)
    (testing "Is able to detect non working git"
      (is (not (sut/git-installed?* "non-git-command"))))))

(comment
  (sut/clean-hard "")
  (let [tmp-dir (build-files/create-temp-dir "test")]
    (build-files/create-dirs tmp-dir)
    (sut/clone-repo-branch tmp-dir
                           "git@github.com:hephaistox/automaton-foobar.git"
                           "main")
    (sut/create-and-switch-to-branch tmp-dir "test-branch")
    (println "branch is correct?:"
             (= "test-branch" (sut/current-branch tmp-dir))))
  (try
    (sut/push-local-dir-to-repo
     {:source-dir
      "/Users/anthonycaumond/Dev/hephaistox/monorepo/clojure/automaton/automaton_core"
      :repo-address "git@github.com:hephaistox/automaton-foobar.git"
      :base-branch-name "main"
      :commit-msg "Manual test"
      :target-branch "caumond/feature/core-is-autonomous_2"})
    (catch Exception e (println e)))
  ;
)

(deftest find-git-repo-test
  (testing "The current code should be in a git repo"
    (when-not (build-cicd-server/is-cicd?)
      (is (not (str/blank? (sut/find-git-repo "")))))))

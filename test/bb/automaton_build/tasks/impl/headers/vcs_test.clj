(ns automaton-build.tasks.impl.headers.vcs-test
  (:require
   [automaton-build.os.cmds                :as build-commands]
   [automaton-build.os.file                :as build-file]
   [automaton-build.tasks.impl.headers.vcs :as sut]
   [clojure.string                         :as str]
   [clojure.test                           :refer [deftest is]]))

(deftest remote-branches-test
  (is (re-find #"Remote branch has failed"
               (let [s (new java.io.StringWriter)]
                 (binding [*out* s]
                   (sut/remote-branches "git@github.com:hephaistox/non-existing-repo.git" false)
                   (str s))))
      "A non existing repo returns an error message.")
  (is (str/blank? (let [s (new java.io.StringWriter)]
                    (binding [*out* s]
                      (sut/remote-branches "git@github.com:hephaistox/test-repo.git" false)
                      (str s))))
      "An existing repo is returning no message if successful.")
  (is (= ["origin/main"] (sut/remote-branches "git@github.com:hephaistox/test-repo.git" false))
      "Only one branch exists on this repo."))

(deftest clone-repo-branch-test
  (is (->> (with-out-str (sut/clone-repo-branch (build-file/create-temp-dir)
                                                "git@github.com:hephaistox/non-existing-repo.git"
                                                "main"
                                                true))
           (re-find #"Repository not found"))
      "A wrong repo returns an error message.")
  (is (->> (with-out-str (sut/clone-repo-branch (build-file/create-temp-dir)
                                                "git@github.com:hephaistox/test-repo.git"
                                                "main"
                                                true))
           (re-find #"Branch `main` cloning is successfull."))
      "Is the repo succesfully cloned."))

(deftest new-branch-and-switch-chain-cmd-test
  (is (let [tmp-dir (build-file/create-temp-dir)]
        (binding [*out* (new java.io.StringWriter)]
          (when
            (sut/clone-repo-branch tmp-dir "git@github.com:hephaistox/test-repo.git" "main" false)
            (-> (sut/new-branch-and-switch tmp-dir "foo" false)
                build-commands/first-failing
                build-commands/success))))
      "A non existing branch is created."))

(deftest create-empty-branch-test
  (is
   (=
    "[37m *  Clone repo with branch `main`\n[1A[0K[32m >  Branch `main` cloning is successfull.\n"
    (with-out-str (sut/create-empty-branch (build-file/create-temp-dir)
                                           "git@github.com:hephaistox/test-repo.git" "foo"
                                           "main" false)))
   "An existing repo prints cloning messages only.")
  (is (string? (sut/create-empty-branch (build-file/create-temp-dir)
                                        "git@github.com:hephaistox/test-repo.git" "foo"
                                        "main" false))
      "An existing repo is downloaded")
  (is (re-find #"Impossible to clone."
               (with-out-str (sut/create-empty-branch (build-file/create-temp-dir)
                                                      "git@github.com:hephaistox/test-repo.git"
                                                      "foo"
                                                      "master" false)))
      "A non existing base dir is said to be impossible to clone."))

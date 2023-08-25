(ns automaton-build.adapters.cfg-mgt-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.cfg-mgt :as sut]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.log :as log]
   [automaton-build.env-setup :as env-setup]))

(deftest git-installed?*-test
  (testing "Is able to detect non working git"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Git is not working, aborting, is expecting command"
                          (sut/git-installed?* "non-git-command")))))

(deftest gh-installed*-test
  (testing "Is able to detect non working gh"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Github cli is not working, aborting, is expecting command. Run `brew install gh` and `gh auth login`"
                          (sut/gh-installed?* "non-gh-command")))))

(comment
  (deftest git-installed?-test
    (testing "Is git cli installed?"
      (is (sut/git-installed?*))
      (is (sut/git-installed?))))

  (deftest gh-installed?-test
    (testing "Is github installed?"
      (is (sut/gh-installed?*))
      (is (sut/gh-installed?)))))

(comment
;;  Test clean-to-repo, !! BE careful !!, this deletes your current changes
  (sut/clean-to-repo "..")
  ;
  )

(defn test-a-remote-repo-creation
  "Test a remote repository creation"
  [dir orgname]
  (sut/init dir)
  (try
    (sut/create-remote-repo dir
                            orgname)
    (catch Exception e
      (log/trace "Creation failing due to " e)))
  (files/spit-file (files/create-file-path dir
                                           "foo")
                   "bar")
  (sut/push-repo dir "cfg-mgt unit test"))

(def repo-to-create-name
  "template-app-test")

(def repo-to-create-orgname
  (str/join "/" [(get-in env-setup/env-setup
                         [:container-repo :account]) repo-to-create-name]))

(def repo-to-create-dir
  (files/create-temp-dir))

(comment
  ;; Test the creation and suppression of a repo, gh should be without template-app-test repo after that
  (test-a-remote-repo-creation repo-to-create-dir
                               repo-to-create-orgname)
  (sut/delete-remote-repo repo-to-create-orgname
                          repo-to-create-dir)
;
  )

;; Commented out as we move to subprojects
#_(deftest extract-remote-test
    (testing "Accepts empty message"
      (is (nil?
           (sut/extract-remote "git@github.com:hephaistox/automaton.git" "origin"))))
    (testing "Accepts origin and address on real life example"
      (is (= "git@github.com:hephaistox/automaton.git"
             (sut/extract-remote " origin	git@github.com:hephaistox/automaton.git (fetch)\n origin	git@github.com:hephaistox/automaton.git (push)\n" "origin"))))
    (testing "Fetch and push are distinguished"
      (is (= "git@github.com:hephaistox/automaton.git"
             (sut/extract-remote " origin	git@github.com:hephaistox/automaton.git (fetch)\n origin	git@github.com:hephaistox/automaton.git (push)\n" "origin"))))
    (testing "Testing a different remote name"
      (is (= "git@github.com:hephaistox/automaton.git"
             (sut/extract-remote " foo	git@github.com:hephaistox/automaton.git (fetch)\n origin	git@github.com:hephaistox/automaton.git (push)\n" "foo")))))

(def remote-repo-cloned-dir
  (files/create-temp-dir))

(def remote-repo-url-test
  "git@github.com:hephaistox/ex-scheduling.git")

(def test-file
  (files/create-file-path remote-repo-cloned-dir "to-change-git-status"))

(def freshly-created-repo-dir
  (files/create-temp-dir))

(comment
  (sut/init freshly-created-repo-dir)
  (sut/clone-repo remote-repo-cloned-dir
                  remote-repo-url-test
                  "main")

  (testing "Test an init repo creation,"
    (is (sut/is-working-tree-clean? freshly-created-repo-dir)))

  (testing "The cloned repository should return the cloned branch"
    (is (= remote-repo-url-test
           (sut/get-remote remote-repo-cloned-dir
                           "origin"))))

  (testing "Repeating the already existing branch is ok"
    (is (= :already-ok
           (sut/set-origin-to remote-repo-cloned-dir
                              remote-repo-url-test
                              "origin"))))
  (testing "Changing to another remote alias is ok"
    (is (= :added
           (sut/set-origin-to remote-repo-cloned-dir
                              remote-repo-url-test
                              "foo-test")))
    (is (= remote-repo-url-test
           (sut/get-remote remote-repo-cloned-dir
                           "foo-test"))))

  (testing "Clone a repository"
    (is (not (files/empty-path? remote-repo-cloned-dir))))

  (testing "A fresh repo is clean"
    (is (sut/is-working-tree-clean? remote-repo-cloned-dir)))
  (files/spit-file test-file
                   "To change git status")
  (testing "A repo with some modifications are not cleaned"
    (is (not (sut/is-working-tree-clean? remote-repo-cloned-dir))))

  (testing "Testing current branch"
    (is (= "master"
           (sut/current-branch remote-repo-cloned-dir))))

  (deftest push-repo-test
    ;; See above the `test-a-remote-repo-creation` function for push-repo test
    )

  (deftest get-commit-id-test
    (let [commit-id (sut/get-commit-id remote-repo-cloned-dir)]
      (testing "Test a git commit id is returned"
        (is (string? commit-id))
        (is (= 40
               (count commit-id))))))

  (deftest create-branch-test
    (testing "Test a non clean state"
      (files/spit-file test-file
                       "To change git status")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo

                            (sut/create-branch remote-repo-cloned-dir
                                               "test-branch"
                                               "main")))
      (files/delete-files [test-file])
      (sut/create-branch remote-repo-cloned-dir
                         "test-branch"
                         "main")
      (is (= "test-branch"
             (sut/current-branch remote-repo-cloned-dir)))))

  (deftest get-user-name-test
    (testing "Git username found"
      (let [cfg-name (sut/get-user-name)]
        (is (string? cfg-name))
        (is (not (str/blank? cfg-name))))))

  (deftest create-feature-branch-test
    (files/delete-files [test-file])
    (sut/create-feature-branch remote-repo-cloned-dir
                               "test-branch"
                               "main")
    (is (str/ends-with? (sut/current-branch remote-repo-cloned-dir)
                        "/feature/test-branch")))

  (deftest change-local-branch-test
    (testing "A non existing branch is detected and created"
      (is (not (sut/change-local-branch remote-repo-cloned-dir
                                        "non-existing-branch"))))
    (testing "A existing branch is detected"
      (is (sut/change-local-branch remote-repo-cloned-dir
                                   "main")))))

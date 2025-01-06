(ns automaton-build.code.vcs-test
  (:require
   [automaton-build.code.vcs                :as sut]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd
                                                    chain-cmds
                                                    first-failing
                                                    force-dirs
                                                    success]]
   [clojure.string                          :as str]
   [clojure.test                            :refer [deftest is]]))

(deftest repo-url-regexp-test
  (is (first (re-find (sut/repo-url-regexp) "git@github.com:hephaistox/test-repo.git"))
      "Is \"git@github.com:hephaistox/test-repo.git\" is accepted as a repo-url."))

(deftest latest-commit-message-cmd-test
  (is (= ["" 0 false]
         ((juxt :err :exit (comp str/blank? :out))
          (-> (sut/latest-commit-message-cmd)
              (blocking-cmd "." "Unexpected error during commit message test" false))))
      "Is the log message return no error, a zero exit code and a message."))

(deftest latest-commit-sha-cmd-test
  (is (= [0 ""]
         ((juxt :exit :err)
          (-> (sut/latest-commit-sha-cmd)
              (blocking-cmd "." "Unexpected error during commit sha." false)
              (dissoc :dir))))
      "Is the sha commit return a succesful exit code and no error message"))

(deftest current-branch-test
  (is
   (string? (-> (sut/current-branch-cmd)
                (blocking-cmd "." "Unexpected error during current branch test." false)
                sut/current-branch-analyze))
   "Is current-branch executed with `curent-branch-analyze` returns a string, the current branch."))

(deftest git-changes?-test
  (is (or true
          (-> (sut/git-changes?-cmd)
              (blocking-cmd "." "Unexpected error git-changes? test." false)
              sut/git-changes?-analyze))
      "The git changes executes without exception."))

(deftest clone-file-chain-cmd-test
  (let [file-path (build-file/create-temp-dir "test_vcs")
        file-name "README.md"
        chain-cmd (sut/clone-file-chain-cmd "git@github.com:hephaistox/test-repo.git" file-path
                                            "main" file-name)
        chain-res (chain-cmds chain-cmd "Unexpected error during" false)
        file-desc (-> file-path
                      (build-filename/create-file-path file-name)
                      build-file/read-file)]
    (is (every? :exit chain-res) "Check the commands have been successful")
    (is (nil? (:exception file-desc)) "No error has been found.")
    (is (string? (:raw-content file-desc)) "Check the commands have been successful")))

(deftest shallow-clone-repo-branch-test
  (is (success (let [dir (build-file/create-temp-dir "vcs_test")]
                 (-> (sut/shallow-clone-repo-branch-cmd "git@github.com:hephaistox/test-repo.git"
                                                        "main")
                     (blocking-cmd dir "Unexpected error during shallow-clone-repo" false))))
      "Cloning a simple repo is succesful.")
  (let [s (new java.io.StringWriter)]
    (binding [*out* s]
      (is (:inexisting-remote-branch
           (let [dir (build-file/create-temp-dir "vcs_test")]
             (-> (sut/shallow-clone-repo-branch-cmd "git@github.com:hephaistox/test-repo.git"
                                                    "non-existing-branch")
                 (blocking-cmd dir "Unexpected error during shallow-clone-repo 2" false)
                 sut/shallow-clone-repo-branch-analyze)))
          "Cloning a wrong branch is returning `:inexisting-remote-branch`.")))
  (let [s (new java.io.StringWriter)]
    (binding [*out* s]
      (is (:repository-not-found
           (let [dir (build-file/create-temp-dir "vcs_test")]
             (-> (sut/shallow-clone-repo-branch-cmd "git@github.com/hephaistox/unknown-repo" "main")
                 (blocking-cmd dir "Unexpected error during shallow-clone-repo 2" false)
                 sut/shallow-clone-repo-branch-analyze)))
          "Cloning an unknown `repo-url` is returning `:repository-not-found`."))))

(deftest push-cmd-test
  (let [s (new java.io.StringWriter)
        dir (build-file/create-temp-dir)]
    (-> (sut/shallow-clone-repo-branch-cmd "git@github.com:hephaistox/test-repo.git" "main")
        (blocking-cmd dir "Should not appear, from vcs-test" false))
    (binding [*out* s]
      (is (-> (sut/push-cmd "main" false)
              (blocking-cmd dir "Should not appear, test vcs-test" true)
              sut/push-analyze
              :nothing-to-push)))))

(deftest remote-branches-test
  (is (-> (sut/remote-branches-chain-cmd "git@github.com:hephaistox/test-repo.git")
          (force-dirs (build-file/create-temp-dir "vcs_test"))
          (chain-cmds "Unexpected error during remote-branches test." false)
          sut/remote-branches
          (sut/remote-branch-exists? "main"))
      "Are remote branches returned?"))

(deftest commit-chain-cmd-test
  (let [dir (build-file/create-temp-dir)]
    (-> (sut/shallow-clone-repo-branch-cmd "git@github.com:hephaistox/test-repo.git" "main")
        (blocking-cmd dir "Should not appear" false))
    (is (= [true true]
           (let [s (new java.io.StringWriter)]
             (binding [*out* s]
               (-> (sut/commit-chain-cmd "try to commit nothing")
                   (force-dirs dir)
                   (chain-cmds
                    "Unexpected error during commit chain cmd test - during nothing to commit test"
                    false)
                   first-failing
                   sut/commit-analyze
                   (select-keys [:is-commit :nothing-to-commit])
                   vals))))
        "Detects the commit has been executed and nothing has been commited.")
    (-> (build-filename/create-file-path dir "README.md")
        (build-file/write-file "testing commit"))
    (is (= [true]
           (-> (sut/commit-chain-cmd "try to commit nothing")
               (force-dirs dir)
               (chain-cmds "Unexpected error during commit chain cmd test - during commit test"
                           false)
               first-failing
               sut/commit-analyze
               (select-keys [:is-commit :nothing-to-commit])
               vals))
        "A modification is detected and the nothing-to-commit is false.")))

(deftest current-repo-url-test
  (is (zero? (-> (sut/current-repo-url-cmd)
                 (blocking-cmd "" "test current repo url failed" true)
                 :exit))
      "Is the execution ok?")
  (is
   (=
    "git@github.com:hephaistox/automaton-build.git"
    (->
      {:exit 0
       :out
       "origin git@github.com:hephaistox/automaton-build.git (fetch)
origin  git@github.com:hephaistox/automaton-build.git (push)"}
      sut/current-repo-url-analyze))
   "Is the analyzis finding finding the url?"))

(comment
  (-> (sut/current-tag-cmd)
      (blocking-cmd "." "Unexpected error during commit message test" false)
      sut/current-tag-analyze))

(deftest gh-run-wip?-test
  (comment
    (-> (sut/gh-run-wip?-cmd)
        (blocking-cmd "" "Unable to execute `gh` run wip" false)
        #_sut/gh-run-wip?-analyze)
    "completed\tfailure\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622334211\t36s\t2025-01-05T19:25:35Z"
    "completed\tsuccess\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622284876\t1m43s\t2025-01-05T19:17:40Z")
  (is
   (=
    {:status :run-failed
     :run-id "12622334211"}
    (->
      {:exit 0
       :out
       "completed\tfailure\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622334211\t36s\t2025-01-05T19:25:35Z"}
      sut/gh-run-wip?-analyze))
   "A failure")
  (is
   (=
    {:status :run-ok
     :run-id "12622284876"}
    (->
      {:exit 0
       :out
       "completed\tsuccess\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622284876\t1m43s\t2025-01-05T19:17:40Z"}
      sut/gh-run-wip?-analyze))
   "A success")
  (is
   (=
    {:run-id "12622284876"
     :status :wip}
    (->
      {:exit 0
       :out
       "aze\twip\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622284876\t1m43s\t2025-01-05T19:17:40Z"}
      sut/gh-run-wip?-analyze))
   "A wip"))

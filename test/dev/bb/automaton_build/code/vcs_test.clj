(ns automaton-build.code.vcs-test
  (:require
   [automaton-build.code.vcs :as sut]
   [automaton-build.os.cmd   :refer [as-string]]
   [automaton-build.os.file  :as build-file]
   [clojure.test             :refer [deftest is]]))

;; Initialize a repo
;; ********************************************************************************
(deftest init-vcs-test
  (let [dir (build-file/create-temp-dir)]
    (is (= {:status :success
            :out-stream []
            :err-stream []}
           (-> (sut/init-vcs-cmd)
               (as-string dir)
               (select-keys [:status :out-stream :err-stream]))))))

(deftest remove-pager-cmd
  (let [dir (build-file/create-temp-dir)]
    (-> (sut/init-vcs-cmd)
        (as-string dir))
    (is (= {:status :success
            :out-stream []
            :err-stream []}
           (-> (sut/remove-pager-cmd)
               (as-string dir)
               (select-keys [:status :out-stream :err-stream]))))))

(deftest add-origin-cmd
  (let [dir (build-file/create-temp-dir)]
    (-> (sut/init-vcs-cmd)
        (as-string dir))
    (is (= {:status :success
            :out-stream []
            :err-stream []}
           (-> (sut/add-origin-cmd "http://noop.com/noop")
               (as-string dir)
               (select-keys [:status :out-stream :err-stream]))))))

(deftest clone-cmd-test
  (let [dir (build-file/create-temp-dir)]
    (is (= {:status :success
            :out-stream []
            :err-stream ["Cloning into '.'..."]}
           (-> (sut/clone-cmd "main" "git@github.com:hephaistox/test-repo.git")
               (as-string dir)
               (select-keys [:status :out-stream :err-stream]))))))

(deftest fetch-origin-cmd-test
  (let [dir (build-file/create-temp-dir)]
    (-> (sut/clone-cmd "main" "git@github.com:hephaistox/test-repo.git")
        (as-string dir))
    (is (= {:status :success
            :out-stream []
            :err-stream []}
           (-> (sut/fetch-origin-cmd)
               (as-string dir)
               (select-keys [:status :out-stream :err-stream]))))))

(deftest shallow-clone-cmd-test
  (let [dir (build-file/create-temp-dir)]
    (is (= {:status :success
            :out-stream []
            :err-stream ["Cloning into 'test-repo'..."]}
           (-> (sut/shallow-clone-cmd "main" "git@github.com:hephaistox/test-repo.git")
               (as-string dir)
               (select-keys [:status :out-stream :err-stream]))))))

;; Get data from remote repo
;; ********************************************************************************

;; Remote repository
;; ********************************************************************************

(deftest current-repo-url-test
  (is (string? (-> (sut/current-repo-url-cmd)
                   (as-string "")
                   sut/current-repo-url-analyze))))

;; Committing
;; ********************************************************************************

(deftest git-changes?-test
  (is (= :success
         (-> (sut/git-changes?-cmd)
             (as-string ".")
             :status))
      "The git changes executes without exception."))

(deftest latest-commit-message-cmd-test
  (is (= :success
         (-> (sut/latest-commit-message-cmd)
             (as-string ".")
             :status))
      "The git changes executes without exception."))

(deftest latest-commit-sha-cmd-test
  (is (= :success
         (-> (sut/latest-commit-sha-cmd)
             (as-string ".")
             :status))
      "The git changes executes without exception."))

;; Branches
;; ********************************************************************************

(deftest current-branch-cmd-test
  (is (string? (-> (sut/current-branch-cmd)
                   (as-string "")
                   sut/current-branch-analyze))))

(deftest current-branch-analyze-test
  (is (string? (-> (sut/current-branch-cmd)
                   (as-string "")
                   sut/current-branch-analyze))))

(deftest current-branch-test
  (is
   (string? (-> (sut/current-branch-cmd)
                (as-string ".")
                sut/current-branch-analyze))
   "Is current-branch executed with `curent-branch-analyze` returns a string, the current branch."))

;; Tagging
;; ********************************************************************************
(comment
  (is (-> (sut/current-tag-cmd)
          (as-string ".")
          sut/current-tag-analyze)))
;; Cleaning
;; ********************************************************************************

;; VCS distant run
;; ********************************************************************************

(deftest gh-run-wip?-test
  (comment
    (-> (sut/gh-run-wip?-cmd)
        (as-string "")
        #_sut/gh-run-wip?-analyze)
    "completed\tfailure\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622334211\t36s\t2025-01-05T19:25:35Z"
    "completed\tsuccess\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622284876\t1m43s\t2025-01-05T19:17:40Z")
  (is
   (=
    {:status :run-failed
     :run-id "12622334211"}
    (->
      {:status :success
       :out-stream
       ["completed\tfailure\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622334211\t36s\t2025-01-05T19:25:35Z"]}
      sut/gh-run-wip?-analyze))
   "A failure")
  (is
   (=
    {:status :run-ok
     :run-id "12622284876"}
    (->
      {:status :success
       :out-stream
       ["completed\tsuccess\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622284876\t1m43s\t2025-01-05T19:17:40Z"]}
      sut/gh-run-wip?-analyze))
   "A success")
  (is
   (=
    {:run-id "12622284876"
     :status :wip}
    (->
      {:status :success
       :out-stream
       ["aze\twip\tClean build\tCommit validation - Delaguardo flavor\tclean_build\tpush\t12622284876\t1m43s\t2025-01-05T19:17:40Z"]}
      sut/gh-run-wip?-analyze))
   "A wip"))

;; Various
;; ********************************************************************************

(deftest repo-url-regexp-test
  (is (first (re-find (sut/repo-url-regexp) "git@github.com:hephaistox/test-repo.git"))
      "Is \"git@github.com:hephaistox/test-repo.git\" is accepted as a repo-url."))

(ns automaton-build.adapters.container-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.container :as sut]
   [automaton-build.adapters.deps-edn :as deps-edn]
   [automaton-build.adapters.files :as files]
   [automaton-build.env-setup :as env-setup]))

(def test-container-name
  "test-container")

(def test-account
  (get-in env-setup/env-setup
          [:container-repo :account]))

(def container-src-dir
  (files/extract-path (-> (io/resource "test-container")
                          io/as-file
                          str)))

(def test-assembled-container-dir
  (files/create-dir-path))

;; Test section

(deftest container-installed?*-test
  (testing "Is able to detect non working docker"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Docker is not working"
                          (sut/container-installed? "non-container-command")))))

(comment
  ;; Executes a story of the test container creation

  (sut/container-clean)
  (= 1 ;;Title only
     (count
      (str/split-lines
       (sut/container-image-list))))

  (sut/build-container-image test-container-name
                             container-src-dir)
  (= 2 ;;Title  and test-container
     (count
      (str/split-lines
       (sut/container-image-list))))

  (sut/push-container test-container-name
                      test-account)

  (sut/container-interactive test-container-name
                             ".")

  (files/delete-files [test-assembled-container-dir])

  (sut/build-and-push-image test-container-name
                            test-account
                            container-src-dir
                            test-assembled-container-dir
                            [(deps-edn/get-deps-filename ".")]  ;; The deps.edn of the current REPL
                            true)
  ;
  )

(ns automaton-build.cli-params-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.cli-params :as sut]))

(def apps-stub
  [{:cust-app? true
    :app-name "app-stub"
    :app-dir "app_stub"
    :dir "app_stub"}
   {:cust-app? false
    :app-name "everything"
    :app-dir "everything"
    :dir "everthing"
    :everything? true}
   {:app-name "non-cust-app"
    :app-dir "non-cust-app"
    :dir "non_cust_app"}
   {:app-dir "base_app"
    :app-name "base-app"}
   {:build? true
    :app-dir "builbuildd"
    :app-name "build"}])

(deftest create-task-params
  (testing "Mode `:non-existing-mode`: a non existing mode is caught"
    (is (re-find #"No matching case for"
                 (sut/create-task-params apps-stub :non-existing-mode
                                         {:task-name "task-stub"
                                          :first-param nil
                                          :second-param nil}))))
  (testing "Mode `:none`: mode accepts no argument"
    (is (= {}
           (sut/create-task-params apps-stub :none
                                   {:task-name "task-stub"
                                    :first-param nil
                                    :second-param nil}))))

  (testing "Mode `:none`: wrong number of argument is not validated"
    (is (re-find #"No parameter is expected"
                 (sut/create-task-params apps-stub :none
                                         {:task-name "task-stub"
                                          :first-param "unwanted-app1"
                                          :second-param ""}))))

  (testing "Mode `:one-app-but-everything`: one app name required and found"
    (is (= {:app-name "app-stub"}
           (sut/create-task-params apps-stub :one-app-but-everything
                                   {:task-name "task-stub"
                                    :first-param "app-stub"
                                    :second-param nil}))))

  (testing "Mode `:one-app-but-everything`: everything is not accepted"
    (is (re-find #"App .* is not a known app"
                 (sut/create-task-params apps-stub :one-app-but-everything
                                         {:task-name "task-stub"
                                          :first-param "everything"
                                          :second-param nil}))))

  (testing "Mode `:one-app-but-everything`: the app name does not exist"
    (is (re-find #"App .* is not a known app"
                 (sut/create-task-params apps-stub :one-app-but-everything
                                         {:task-name "task-stub"
                                          :first-param "non-existing-app"
                                          :second-param nil}))))

  (testing "Mode `:one-app-but-everything`: wrong number of arguments found"
    (is (re-find #"app-name is one of"
                 (sut/create-task-params apps-stub :one-app-but-everything
                                         {:task-name "task-stub"
                                          :first-param "non-existing-app"
                                          :second-param  "non-existing-app"})))
    (is (re-find #"app-name is one of"
                 (sut/create-task-params apps-stub :one-app-but-everything
                                         {:task-name "task-stub"
                                          :first-param nil
                                          :second-param nil}))))

  (testing "Mode `:one-cust-app`: one cust-app name required and found"
    (is (= {:cust-app-name "app-stub"}
           (sut/create-task-params apps-stub :one-cust-app
                                   {:task-name "task-stub"
                                    :first-param "app-stub"
                                    :second-param nil}))))

  (testing "Mode `:one-cust-app`: non cust-app given "
    (is (re-find #"not a known app"
                 (sut/create-task-params apps-stub :one-cust-app
                                         {:task-name "task-stub"
                                          :first-param "non-cust-app"
                                          :second-param nil}))))

  (testing "Mode `:one-cust-app`: app name does not exist"
    (is (re-find #"not a known app"
                 (sut/create-task-params apps-stub :one-cust-app
                                         {:task-name "task-stub"
                                          :first-param "non-existing-required-name"
                                          :second-param nil}))))

  (testing "Mode `:one-cust-app`: wrong number of arguments found"
    (is (re-find #"where app-name is one of"
                 (sut/create-task-params apps-stub  :one-cust-app
                                         {:task-name "task-stub"
                                          :first-param "app-stub"
                                          :second-param "fake-app"})))
    (is (re-find #"where app-name is one of"
                 (sut/create-task-params apps-stub  :one-cust-app
                                         {:task-name "task-stub"
                                          :first-param ""
                                          :second-param ""}))))

  (testing "Mode `:one-cust-app-with-commit`: one cust-app name required and found"
    (is (= {:cust-app-name "app-stub", :commit-msg "message"}
           (sut/create-task-params apps-stub  :one-cust-app-with-commit
                                   {:task-name "task-stub"
                                    :first-param "app-stub"
                                    :second-param "message"}))))

  (testing "Mode `:one-cust-app-with-commit`: a non cust-app given "
    (is (re-find #"Cust-app .* is not a known app"
                 (sut/create-task-params apps-stub  :one-cust-app-with-commit
                                         {:task-name "task-stub"
                                          :first-param "non-cust-app"
                                          :second-param "message"}))))

  (testing "Mode `:one-cust-app-with-commit`: the app name does not exist"
    (is (re-find #"Cust-app .* is not a known app"
                 (sut/create-task-params apps-stub  :one-cust-app-with-commit
                                         {:task-name "task-stub"
                                          :first-param "non-existing-required-name"
                                          :second-param "message"}))))

  (testing "Mode `:one-cust-app-with-commit`: wrong number of arguments found"
    (is (re-find #"commit string\", where commit string is a non empty"
                 (sut/create-task-params apps-stub  :one-cust-app-with-commit
                                         {:task-name "task-stub"
                                          :first-param "app-stub"
                                          :second-param nil})))
    (is (re-find #"commit string\", where commit string is a non empty"
                 (sut/create-task-params apps-stub  :one-cust-app-with-commit
                                         {:task-name "task-stub"
                                          :first-param "app-stub"
                                          :second-param ""})))
    (is (re-find #"commit string\", where commit string is a non empty"
                 (sut/create-task-params apps-stub  :one-cust-app-with-commit
                                         {:task-name "task-stub"
                                          :first-param ""
                                          :second-param ""}))))

  (testing "Mode `:one-not-app-names`: one non existing application name required and found"
    (is (= {:cust-app-name "non-existing-app"}
           (sut/create-task-params apps-stub  :one-not-app-names
                                   {:task-name "task-stub"
                                    :first-param "non-existing-app"
                                    :second-param nil}))))

  (testing "Mode `:one-not-app-names`: app already existing"
    (is (re-find #"App .* is already defined, please don't use any of those names"
                 (sut/create-task-params apps-stub  :one-not-app-names
                                         {:task-name "task-stub"
                                          :first-param "app-stub"
                                          :second-param nil}))))

  (testing "Mode `:one-container-image`: one container image name required and found"
    (is (= {:container-image-name "gha-image"}
           (sut/create-task-params apps-stub :one-container-image
                                   {:task-name "task-stub"
                                    :first-param "gha-image"
                                    :second-param nil}))))

  (testing "Mode `:one-container-image`: non existing image is found"
    (is (re-find #"Container image .* is not a known container image"
                 (sut/create-task-params apps-stub :one-container-image
                                         {:task-name "task-stub"
                                          :first-param "container-non-existing-image"
                                          :second-param nil}))))

  (testing "Mode `:one-container-image`: but wrong number of arguments found"
    (is (re-find #"Usage: bb .*, where local is one "
                 (sut/create-task-params apps-stub :one-container-image
                                         {:task-name "task-stub"
                                          :first-param nil
                                          :second-param nil}))))

  (testing "Mode `:one-cust-app-or-everything`: one cust-app is validated"
    (is (= {:one-cust-app-or-everything "app-stub"}
           (sut/create-task-params apps-stub :one-cust-app-or-everything
                                   {:task-name "task-stub"
                                    :first-param "app-stub"
                                    :second-param nil}))))
  (testing "Mode `:one-cust-app-or-everything`: everything is validated"
    (is (= {:one-cust-app-or-everything "everything"}
           (sut/create-task-params apps-stub :one-cust-app-or-everything
                                   {:task-name "task-stub"
                                    :first-param "everything"
                                    :second-param nil}))))

  (testing "Mode `:one-cust-app-or-everything`: wrong  number of arguments found"
    (is (re-find #"Cust-app should not be followed by a second parameter,"
                 (sut/create-task-params apps-stub :one-cust-app-or-everything
                                         {:task-name "task-stub"
                                          :first-param nil
                                          :second-param nil})))
    (is (re-find #"Cust-app should not be followed by a second parameter,"
                 (sut/create-task-params apps-stub :one-cust-app-or-everything
                                         {:task-name "task-stub"
                                          :first-param "wrong-app-1"
                                          :second-param "wrong-app-2"}))))

  (testing "Mode `:one-cust-app-or-everything`: no cust app nor everything is provided"
    (is (re-find #"is not a know cust-app"
                 (sut/create-task-params apps-stub :one-cust-app-or-everything
                                         {:task-name "task-stub"
                                          :first-param "non-cust-app-nor-everything"
                                          :second-param nil})))))

(ns automaton-build.container.github-action-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]

   [automaton-build.container.github-action :as sut]
   [automaton-build.adapters.container :as adapter-container]
   [automaton-build.app.find-an-app-for-test :as bafaaft]))

(deftest image-dir-test
  (testing "image directory"
    (is (not (str/blank? sut/image-src-dir)))))

(deftest create-cc-image-name-test
  (testing "gha image name"
    (is (not (str/blank? (sut/create-gha-image-name "foo"))))))

(def one-cust-app
  bafaaft/one-cust-app)

(def one-cust-app-name
  (:app-name one-cust-app))

(comment
  (adapter-container/container-clean)

  (sut/build one-cust-app-name
             one-cust-app-name)

  (sut/connect one-cust-app-name
               one-cust-app-name
               "..")
;
  )

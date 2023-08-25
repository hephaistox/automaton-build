(ns automaton-build.container.clever-cloud-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]

   [automaton-build.container.clever-cloud :as sut]

   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.adapters.container :as adapter-container]))

(deftest image-dir-test
  (testing "image directory"
    (is (not (str/blank? sut/image-src-dir)))))

(deftest create-cc-image-name-test
  (testing "cc image name"
    (is (string? (sut/create-cc-image-name "foo")))))

(def one-cust-app-name
  (:app-name bafaaft/one-cust-app))

(def one-cust-app-dir
  (:app-dir bafaaft/one-cust-app))

(comment
  (adapter-container/container-clean)

  (sut/build one-cust-app-name
             one-cust-app-dir)

  (sut/connect one-cust-app-name
               one-cust-app-dir
               "..")

;
  )

(ns automaton-build.os.edn-utils-test
  (:require
   [automaton-build.os.edn-utils :as sut]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(def stub-edn (io/resource "os/edn-file.edn"))

(def stub-non-edn (io/resource "README.md"))

(deftest read-edn-test
  (testing "Find stub file"
    (is (= {:foo "bar"
            :bar 1}
           (sut/read-edn stub-edn))))
  (testing "Malformed files are detected "
    (is (nil? (sut/read-edn stub-non-edn))))
  (testing "Non existing files are detected "
    (is (nil? (sut/read-edn "not existing file")))))

(deftest spit-edn-test
  (let [tmp-file (sut/create-tmp-edn "edn-utils-test.edn")]
    (testing "Creates edn file"
      (sut/spit-edn tmp-file {10 20})
      (is (= {10 20} (sut/read-edn tmp-file)))
      (sut/spit-edn tmp-file "{15 25}")
      (is (= {15 25} (sut/read-edn tmp-file)))
      (sut/spit-edn tmp-file {5 5} ";;Header")
      (is (= {5 5} (sut/read-edn tmp-file))))))

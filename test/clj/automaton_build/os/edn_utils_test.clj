(ns automaton-build.os.edn-utils-test
  (:require
   [automaton-build.os.edn-utils :as sut]
   [automaton-build.os.files :as build-files]
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
      (sut/spit-edn tmp-file {5 5} "Header")
      (is (= {5 5} (sut/read-edn tmp-file))))
    (testing "Creating edn file without header argument does not create header"
      (sut/spit-edn tmp-file "a")
      (is (= "a" (build-files/read-file (build-files/absolutize tmp-file)))))
    (testing "No change in content returns false"
      (sut/spit-edn tmp-file {:a 2})
      (is (false? (sut/spit-edn tmp-file {:a 2})))
      (sut/spit-edn tmp-file
                    {:a 2
                     :b "costam"})
      (is (false? (sut/spit-edn tmp-file
                                {:a 2
                                 :b "costam"})))
      (sut/spit-edn tmp-file
                    {:a 2
                     :b "costam"
                     :h nil
                     :e {:d :f
                         :g "whatever forever    "}})
      (is (false? (sut/spit-edn tmp-file
                                {:a 2
                                 :b "costam"
                                 :h nil
                                 :e {:d :f
                                     :g "whatever forever    "}}))))
    (testing "No change in content and change in header returns false"
      (sut/spit-edn tmp-file {:a 2})
      (is (false? (sut/spit-edn tmp-file {:a 2} "New header here hello")))
      (sut/spit-edn tmp-file
                    {:a 2
                     :b "costam"})
      (is (false? (sut/spit-edn tmp-file
                                {:a 2
                                 :b "costam"}
                                "New header here hello")))
      (sut/spit-edn tmp-file
                    {:a 2
                     :b "costam"
                     :h nil
                     :e {:d :f
                         :g "whatever forever    "}})
      (is (false? (sut/spit-edn tmp-file
                                {:a 2
                                 :b "costam"
                                 :h nil
                                 :e {:d :f
                                     :g "whatever forever    "}}
                                "New header here hello"))))
    (testing "When data is incomplete returns nil"
      (is (nil? (sut/spit-edn nil {:a 2})))
      (is (nil? (sut/spit-edn nil {:a 2} nil)))
      (is (nil? (sut/spit-edn tmp-file nil)))
      (is (nil? (sut/spit-edn tmp-file nil nil)))
      (is (nil? (sut/spit-edn nil nil)))
      (is (nil? (sut/spit-edn nil nil nil))))))

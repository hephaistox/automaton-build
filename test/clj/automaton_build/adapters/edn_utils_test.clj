(ns automaton-build.adapters.edn-utils-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.edn-utils :as sut]))

(deftest is-clojure-like-file-test
  (testing "Compatible files are found"
    (is (sut/is-clojure-like-file "foo.clj"))
    (is (sut/is-clojure-like-file "foo.cljc"))
    (is (sut/is-clojure-like-file "foo.cljs")))
  (testing "Incompatible files are found"
    (is (not (sut/is-clojure-like-file "foo.clj ")))
    (is (not (sut/is-clojure-like-file "foo.cljcaze")))
    (is (not (sut/is-clojure-like-file "foo")))
    (is (not (sut/is-clojure-like-file "")))
    (is (not (sut/is-clojure-like-file nil)))))

(deftest read-edn-test
  (testing "Malformed files are detected "
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"File.*is not an edn"
                          (sut/read-edn "non-used-file-name" (fn [_] "1.1.1 (+ 1 1)")))))
  (testing "Non existing files are detected "
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unable to load the file"
                          (sut/read-edn "not existing file"))))
  (testing "Non existing files throw an exception"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unable to load the file"
                          (sut/read-edn "non existing file" slurp))))
  (testing "Non existing files throw an exception"
    (is (= "test1-content"
           (sut/read-edn (io/resource "resource-test-copy-dir/test1"))))))

(deftest read-edn-or-nil-test
  (testing "Skip non existing files and return nil"
    (is (nil? (sut/read-edn-or-nil "non existing file" slurp)))))

(def tmp-file
  ;; => "../tmp/edns/6bfcd070-3c43-11ee-aece-d9f945498185.edn"

  (sut/create-tmp-edn))

(deftest spit-edn-test
  (testing "Creates edn file"
    (let [tmp-file (sut/create-tmp-edn)]
      (sut/spit-edn tmp-file
                    {10 20})
      (is (= {10 20}
             (sut/read-edn-or-nil tmp-file)))
      (sut/spit-edn tmp-file
                    "{15 25}")
      (is (= {15 25}
             (sut/read-edn-or-nil tmp-file)))
      (sut/spit-edn tmp-file
                    {5 5}
                    "Header"))))

(deftest update-edn-content-test
  (testing "Update the edn"
    (let [tmp-file (sut/create-tmp-edn)]
      (sut/spit-edn tmp-file
                    {10 20})
      (is (= {10 25}
             (sut/update-edn-content tmp-file
                                     #(assoc %
                                             10 25)))))))
(comment
  (sut/spit-edn (sut/create-tmp-edn)
                (sut/read-edn "bb.edn")
                "Test")
;
  )

(ns automaton-build.adapters.html-test
  (:require [automaton-build.adapters.html :as sut]
            [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]))

(deftest md->html-str
  (testing "html compiled correctly"
    (is
     (= "<p>Hi I'm your test file <strong>with some shnifty things</strong></p><p>And this is list:</p><ul><li>first element</li><li>second element</li></ul>"
        (sut/md->html-str
         {:md-path (io/resource "test_file.md")})))))

(deftest header-str
  (testing "html-header is created"
    (is
     (= "<img id=\"my-test\" src=\"my-test.jpg\" />"
        (sut/header-str {:logo-path "my-test.jpg"
                         :header-id "my-test"})))))

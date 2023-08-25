(ns automaton-build.adapters.pdf-test
  (:require [automaton-build.adapters.pdf :as sut]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(def test-file
  "test_file.pdf")

(defn test-file-exists?
  []
  (some? (io/resource test-file)))

(deftest img-src
  (testing "img-src string created correctly"
    (is (=
         (#'sut/img-src "anti")
         "src=\"anti\""))))

(deftest url?
  (testing "correctly recognizes http"
    (is
     (= (#'sut/url? "https://mati.com")
        true))
    (is
     (= (#'sut/url? "http://www.mati.com")
        true)))
  (testing "local path is found as not url"
    (is
     (= (#'sut/url? "my/dir/here.png")
        false))
    (is
     (= (#'sut/url? "/this/is/my/file")
        false))
    (is
     (= (#'sut/url? "../this/is/path")
        false))))

(deftest src->accepted-src
  (testing "src local file converted correctly"
    (is (= (str "<img src=\"file:"
                (.getAbsolutePath (java.io.File. ""))
                "/resources/my/file/path.jpg\">")
           (sut/src->accepted-src "resources/" "<img src=\"my/file/path.jpg\">"))))
  (testing "src that is an http is not converted"
    (is
     (= (str "<img src=\"https://www.hephaistox.com\"")
        (sut/src->accepted-src "resources/" "<img src=\"https://www.hephaistox.com\"")))))

(deftest html-str->pdf
  (testing "pdf created from html str"
    (when (test-file-exists?)  (-> test-file io/resource io/delete-file))
    (sut/html-str->pdf {:html-str "<div> Hello </div>"
                        :output-path "automaton/automaton-build/test/resources/test_file.pdf"})
    (is (test-file-exists?))))

(ns automaton-build.adapters.blog-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [automaton-build.blog :as sut]))

(def test-file
  "test_file.pdf")

(defn test-file-exists?
  []
  (some? (io/resource test-file)))

(deftest blog-md->pdf
  (testing "Pdf file was created"
    (when (test-file-exists?)  (-> test-file io/resource io/delete-file))
    (sut/blog-md->pdf {:document-name "test_file"
                       :pdf-metadata {:title "Test file"
                                      :author "Test"
                                      :creator "Test"
                                      :subject "Test subject it is"}
                       :resources-dir ""
                       :md-path "automaton/automaton-build/test/resources/test_file"
                       :pdf-path "automaton/automaton-build/test/resources/test_file"})
    (is (test-file-exists?))))

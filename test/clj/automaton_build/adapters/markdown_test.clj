(ns automaton-build.adapters.markdown-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-build.adapters.markdown :as sut]))

(deftest create-md-test
  (testing "Try to create a markdown file"
    (let [filename (files/create-file-path (files/create-temp-dir)
                                           "test")
          file-content "test"]
      (log/trace "`md` file created: " filename)
      (sut/create-md filename
                     file-content)
      (is (= file-content
             (slurp filename))))))

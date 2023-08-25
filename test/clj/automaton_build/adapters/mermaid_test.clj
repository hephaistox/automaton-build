(ns automaton-build.adapters.mermaid-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-core.adapters.edn-utils :as edn-utils]
   [automaton-build.adapters.mermaid :as sut]))

(deftest need-to-update?
  (let [older-test-file (edn-utils/create-tmp-edn)
        newer-test-file (edn-utils/create-tmp-edn)]
    (spit older-test-file "tmp")
    (spit (str older-test-file ".mermaid") "tmp")
    (Thread/sleep 100)
    (spit newer-test-file "tmp")
    (spit (str newer-test-file ".mermaid") "tmp")
    (testing "The newer file is detected as so"
      (is (sut/need-to-update? (str newer-test-file ".mermaid")
                               older-test-file)))
    (testing "The older file is detected as so"
      (is (not (sut/need-to-update? (str older-test-file ".mermaid")
                                    newer-test-file))))
    (testing "Whatever the age, non mermaid files are ignored"
      (is (not (sut/need-to-update? (str newer-test-file)
                                    older-test-file)))
      (is (not (sut/need-to-update? (str older-test-file)
                                    newer-test-file))))))

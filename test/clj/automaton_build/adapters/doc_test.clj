(ns automaton-build.adapters.doc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [automaton-build.adapters.doc :as sut]))

(deftest doc-dir-test
  (testing "Documentation dir"
    (is (string?
         (sut/doc-subdir "app")))))

(comment
  (sut/build-doc "Monorepo appname"
                 "Monorepository title"
                 "Monorepository desc"
                 "."
                 ["landing/src/clj"
                  "landing/src/cljs"
                  "landing/src/cljc"])
;
  )

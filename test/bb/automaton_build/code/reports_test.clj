(ns automaton-build.code.reports-test
  (:require
   [automaton-build.code.reports :as sut]
   [clojure.test                 :refer [deftest is]]))

(deftest is-ignored-file?-test
  (is (not (sut/is-ignored-file? "Content without the tag are not ignored")))
  (is
   (sut/is-ignored-file?
    "#_{:heph-ignore {:reports false}} (ns automaton-build.tasks.3-test (:require [automaton-build.tasks.3 :as sut] [clojure.test         :refer [deftest is]]))")
   "Content wth the tag are ignored"))

(deftest search-aliases-test
  (is
   (=
    3
    (count
     (sut/search-aliases
      {:raw-content
       "(ns automaton-build.code.files-test\n  (:require\n   [automaton-build.code.files      :as shut]\n   [automaton-build.os.edn-utils-bb :as build-edn]\n   [clojure.test                    :refer [deftest is]]))\n\n"
       :filename "test"})))))

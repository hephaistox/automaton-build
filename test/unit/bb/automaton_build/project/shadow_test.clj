(ns automaton-build.project.shadow-test
  (:require
   [automaton-build.project.shadow :as sut]
   [clojure.java.io                :as io]
   [clojure.test                   :refer [deftest is]]))

(deftest read-from-dir-test
  (is (not (-> (sut/read-from-dir (io/resource "cicd/"))
               :invalid?))
      "Current project has a shadow-cljs")
  (is (-> (sut/read-from-dir "zeaz")
          :invalid?)
      "Invalid directories are caught"))

(deftest build-test
  (is (every? keyword?
              (-> (sut/read-from-dir "")
                  :edn
                  sut/build))
      "Returns the build aliases of the current project."))

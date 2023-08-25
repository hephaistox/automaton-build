(ns automaton-build.adapters.shadow-cljs-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.java.io :as io]

   [automaton-build.adapters.shadow-cljs :as sut]
   [automaton-core.adapters.files :as files]))

(def shadow-cljs-dir
  (files/extract-path (str (io/file (io/resource "touch")))))

(deftest load-shadow-cljs-test
  (testing "Test a map is loaded"
    (is (map? (sut/load-shadow-cljs shadow-cljs-dir)))))

(deftest extract-paths-test
  (testing "Paths are extracted"
    (let [dirs (sut/extract-paths (sut/load-shadow-cljs shadow-cljs-dir))]
      (is (every? string? dirs))
      (is (> (count dirs) 3)))))

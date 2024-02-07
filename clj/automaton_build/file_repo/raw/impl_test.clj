(ns automaton-build.file-repo.raw.impl-test
  (:require
   [automaton-build.file-repo.raw.impl :as sut]
   [clojure.test :refer [deftest is testing]]))

(def raw-file-repo-map
  {"foo.clj" ["This is" " the foo file" " hey!"]
   "foo.edn" ["test"]
   "bar.cljc" ["This is the bar file" " ho ho!"]})

(deftest filter-repo-map-test
  (testing "Filter is ok"
    (is (= "foo.clj"
           (-> (sut/filter-repo-map raw-file-repo-map #(= "foo.clj" (first %)))
               ffirst)))))

(deftest filter-by-extension-test
  (testing "Filtering by extension is ok"
    (is (= "foo.clj"
           (-> (sut/filter-by-extension raw-file-repo-map ["clj"])
               ffirst)))))

(deftest exclude-files-test
  (testing "Exlude all files"
    (is (empty? (sut/exclude-files raw-file-repo-map
                                   #{"foo.clj" "bar.cljc" "foo.edn"})))))

(deftest repo-map-test
  (testing "Able to read files"
    (is (= 2 (count (sut/repo-map ["deps.edn" "build_config.edn"]))))))

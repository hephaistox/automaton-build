(ns automaton-build.file-repo.clj-code-test
  (:require
   [automaton-build.file-repo.clj-code :as sut]
   [automaton-build.file-repo.raw      :as build-filerepo-raw]
   [clojure.test                       :refer [deftest is testing]]))

(def clj-repo-stub (sut/make-clj-repo-from-dirs [""]))

(deftest filter-by-usage-test
  (testing "Filter by usage is ok"
    (is (< 0
           (-> (sut/filter-by-usage clj-repo-stub :edn)
               count)))))

(deftest search-filenames-test
  (testing "Some clojure files are found in the current project"
    (is (< 0
           (-> (sut/search-clj-filenames ".")
               count)))))

(comment
  (prn (-> (sut/filter-by-usage (sut/make-clj-repo-from-dirs [""]) :reader)
           build-filerepo-raw/file-repo-map
           keys))
  ;
)

(deftest make-clj-repo-test
  (testing "test repos for edn"
    (is (< 0
           (-> (sut/make-clj-repo-from-dirs [""] :edn)
               build-filerepo-raw/file-repo-map
               keys
               count))))
  (testing "test repos for clj"
    (is (< 0
           (-> (sut/make-clj-repo-from-dirs [""] :clj)
               build-filerepo-raw/file-repo-map
               keys
               count))))
  (testing "test to optional value :reader"
    (is (< 30
           (count (build-filerepo-raw/file-repo-map (sut/make-clj-repo-from-dirs
                                                     [""])))))))

(comment
  (-> (sut/make-clj-repo-from-dirs ["src" "test"] :clj)
      :_file-repo-map
      count)
  ;
)

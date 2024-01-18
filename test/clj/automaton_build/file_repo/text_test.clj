(ns automaton-build.file-repo.text-test
  (:require
   [automaton-build.file-repo.text :as sut]
   [automaton-build.file-repo.raw :as build-filerepo-raw]
   [clojure.test :refer [deftest is testing]]
   [clojure.string :as str]))

(def files-to-test
  "Should be a list of existing files in all context where that tests are run
with at least two lines in it"
  ["build_config.edn" "bb.edn"])

(def text-file-repo (sut/make-text-file-repo files-to-test))

(deftest exclude-files-test
  (testing "The core map is returned"
    (is (map? (build-filerepo-raw/exclude-files text-file-repo #{})))
    (is (= 1
           (count (build-filerepo-raw/exclude-files text-file-repo
                                                    #{"build_config.edn"}))))))

(deftest file-repo-map-test
  (testing "The core map is returned"
    (is (map? (build-filerepo-raw/file-repo-map text-file-repo)))))

(deftest file-repo-test
  (testing "The core map is returned"
    (is (= 1
           (->> (build-filerepo-raw/filter-repo text-file-repo
                                                #(str/ends-with? (first %)
                                                                 "bb.edn"))
                build-filerepo-raw/file-repo-map
                count)))))

(deftest nb-files-test
  (testing "2 files of files-to-test are found"
    (is (= 2 (build-filerepo-raw/nb-files text-file-repo)))))

(deftest filter-by-extension-test
  (testing ""
    (is (= 1
           (-> (build-filerepo-raw/filter-by-extension text-file-repo #{".edn"})
               count)))))

(deftest make-text-file-map-test
  (testing "build_config.edn and bb.edn are found and loaded"
    (is (= 2
           (-> text-file-repo
               build-filerepo-raw/file-repo-map
               count)))
    (is (every? (fn [file-content] (<= 1 (count file-content)))
                (->> text-file-repo
                     build-filerepo-raw/file-repo-map
                     vals)))))

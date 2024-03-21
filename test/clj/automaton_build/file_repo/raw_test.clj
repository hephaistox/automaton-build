(ns automaton-build.file-repo.raw-test
  (:require
   [automaton-build.file-repo.raw           :as sut]
   [automaton-build.file-repo.raw.impl-test :as sut-raw-impl]
   [clojure.test                            :refer [deftest is testing]]))

(def raw-file-repo (sut/->RawFileRepo sut-raw-impl/raw-file-repo-map))

(deftest exclude-files
  (testing "Exclusion of non existing files is ok"
    (let [res (sut/exclude-files raw-file-repo #{"none"})]
      (is (map? (sut/file-repo-map res)))
      (is (= (sut/file-repo-map raw-file-repo) (sut/file-repo-map res))))))

(deftest file-repo-map-test
  (testing "file repo map actually returns the map"
    (is (map? (sut/file-repo-map raw-file-repo)))))

(deftest filter-repo-test
  (testing "Refusing all files is ok"
    (is (empty? (-> (sut/filter-repo raw-file-repo (constantly false))
                    sut/file-repo-map))))
  (testing "Nil filter function means keep nothing"
    (is (= sut-raw-impl/raw-file-repo-map
           (sut/file-repo-map (sut/filter-repo raw-file-repo nil))))))

(deftest filter-by-extension-test
  (testing "limit to clj files"
    (is (= (select-keys sut-raw-impl/raw-file-repo-map ["foo.clj"])
           (-> (sut/filter-by-extension raw-file-repo #{"clj"})
               sut/file-repo-map)))))

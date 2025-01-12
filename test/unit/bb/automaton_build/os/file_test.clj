(ns automaton-build.os.file-test
  (:require
   [automaton-build.os.file     :as sut]
   [automaton-build.os.filename :as build-filename]
   [clojure.string              :as str]
   [clojure.test                :refer [deftest is testing]]))

;; ********************************************************************************
;; Filenames based on local file structure

(deftest expand-home-str-test
  (is (not (str/includes? (sut/expand-home-str "~/.gitconfig") "~")) "Tilde is replaced.")
  (is (= "env/test/file_found.edn" (sut/expand-home-str "env/test/file_found.edn"))
      "No home is not replaced."))

;; ********************************************************************************
;; Directory manipulation

(deftest is-existing-dir?-test
  (testing "Empty dirs are considered has not existing"
    (is (nil? (sut/is-existing-dir? "")))
    (is (nil? (sut/is-existing-dir? nil))))
  (testing "Directories"
    (is (= "docs" (sut/is-existing-dir? "docs")) "An existing dir")
    (is (nil? (sut/is-existing-dir? "non-existing-dir")) "Not existing dir"))
  (testing "File"
    (is (not (sut/is-existing-dir? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-dir? "non-existing-file.edn")) "Not existing file")))

;; ********************************************************************************
;; File manipulation

(deftest is-existing-file?-test
  (testing "Empty dirs are considered has not existing"
    (is (nil? (sut/is-existing-file? "")))
    (is (nil? (sut/is-existing-file? nil))))
  (testing "File"
    (is (= "deps.edn" (sut/is-existing-file? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-file? "non-existing-file.edn")) "Not existing file"))
  (testing "Directories"
    (is (not (sut/is-existing-file? "src")) "An existing dir")
    (is (nil? (sut/is-existing-file? "non-existing-file.edn")) "Not existing dir")))

;; ********************************************************************************
;; Path manipulation

(deftest is-existing-path?-test
  (testing "Empty paths are considered has non existing"
    (is (nil? (sut/is-existing-path? "")))
    (is (nil? (sut/is-existing-path? nil))))
  (testing "File"
    (is (= "deps.edn" (sut/is-existing-path? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-path? "non-existing-file.edn")) "Not existing file"))
  (testing "Directories"
    (is (= "src" (sut/is-existing-path? "src")) "An existing dir")
    (is (nil? (sut/is-existing-path? "non-existing-file.edn")) "Not existing dir")))

(def root-tmp (sut/create-temp-dir))
(def d (build-filename/create-dir-path root-tmp "test"))
(def d2 (build-filename/create-dir-path root-tmp "test2"))

(def expected-files #{"a" "c" "c/d" "c/d/e" "c/d/e/f" "b"})

(def create-file-path (partial build-filename/create-file-path d))

(deftest path-on-disk?-test
  (sut/ensure-dir-exists d)
  (sut/ensure-dir-exists d2)
  (sut/write-file (create-file-path "a") "a")
  (sut/write-file (create-file-path "b") "b")
  (-> (create-file-path "c" "d" "e")
      (sut/ensure-dir-exists))
  (-> (create-file-path "c" "d" "e" "f")
      (sut/write-file "f"))
  (is (= expected-files
         (->> (sut/search-files d "**" {})
              (map #(build-filename/relativize % d))
              set))
      "Test the expected tree")
  (sut/delete-file (create-file-path "a"))
  (is (= #{{:path "b"
            :exist? true
            :file? true}
           {:path "c"
            :exist? true
            :directory? true}
           {:path "c/d"
            :exist? true
            :directory? true}
           {:path "c/d/e"
            :exist? true
            :directory? true}
           {:path "c/d/e/f"
            :exist? true
            :file? true}}
         (->> (sut/search-files d "**" {})
              (mapv (fn [filename]
                      (-> (sut/path-on-disk filename)
                          (update :path #(build-filename/relativize % d))
                          (dissoc :apath))))
              set))
      "Rich file list contains files/dirs, and nesting."))

;; ********************************************************************************
;; Temporaries

(deftest create-temp-file-test
  (is (string? (sut/create-temp-file "test")))
  (is (-> (sut/create-temp-file "test")
          sut/is-existing-file?)))

;; ********************************************************************************
;; Modify file content

(deftest write-file-test
  (is (= {:filename true
          :afilename true
          :content "foo"
          :status :ok}
         (-> (sut/create-temp-file)
             (sut/write-file "foo")
             (update :filename string?)
             (update :afilename string?)))
      "File properly written")
  (is (= {:filename nil
          :afilename true
          :content "foo"
          :status :fail}
         (-> nil
             (sut/write-file "foo")
             (update :afilename string?)
             (dissoc :exception)))
      "File properly written")
  (is (= #{:filename :afilename :content :status}
         (-> (sut/create-temp-file)
             (sut/write-file "foo")
             keys
             set))
      "Expected keys on error")
  (is (= #{:filename :afilename :exception :content :status}
         (-> nil
             (sut/write-file "foo")
             keys
             set))
      "Expected keys on success"))

(deftest read-file-test
  (let [f (sut/read-file "non-existing-file")]
    (is (= {:filename "non-existing-file"
            :afilename true
            :dir ""
            :invalid? true}
           (-> f
               (dissoc :exception)
               (update :afilename string?)))
        "Non existing file returns invalid?")
    (is (:exception f) "Non existing files contains errors")))

;; ********************************************************************************
;; Search

(deftest copy-actions-test
  (is (->> (sut/search-files d "**" {})
           (mapv (fn [path]
                   (-> path
                       (sut/copy-action d d2))))
           set)))

(deftest do-copy-action-test
  (->> (sut/search-files d "**" {})
       (mapv #(-> %
                  (sut/copy-action d d2)
                  sut/do-copy-action)))
  (is (= #{"test2/c" "test2/c/d" "test2/c/d/e" "test2/c/d/e/f" "test2/b"}
         (->> (sut/search-files d2 "**" {})
              (mapv #(build-filename/relativize % root-tmp))
              set))
      "After the actual copy, all files are found in `d2`"))

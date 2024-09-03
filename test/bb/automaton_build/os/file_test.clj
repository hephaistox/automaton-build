(ns automaton-build.os.file-test
  (:require
   [automaton-build.os.file     :as sut]
   [automaton-build.os.filename :as build-filename]
   [clojure.string              :as str]
   [clojure.test                :refer [deftest is testing]]))

(deftest expand-home-str-test
  (is (not (str/includes? (sut/expand-home-str "~/.gitconfig") "~")) "Tilde is replaced.")
  (is (= "env/test/file_found.edn" (sut/expand-home-str "env/test/file_found.edn"))
      "No home is not replaced."))

(deftest create-temp-file-test
  (is (string? (sut/create-temp-file "test")))
  (is (-> (sut/create-temp-file "test")
          sut/is-existing-file?)))

(deftest is-existing-path?-test
  (testing "Empty paths are considered has non existing"
    (is (nil? (sut/is-existing-path? "")))
    (is (nil? (sut/is-existing-path? nil))))
  (is (sut/is-existing-path? "docs") "Known dir is existing")
  (is (sut/is-existing-path? "deps.edn") "Known files are existing")
  (is (nil? (sut/is-existing-path? "non-existing-dir")) "Non existing dir")
  (is (nil? (sut/is-existing-path? "non-existing-file.edn")) "Non existing file"))

(deftest is-existing-file?-test
  (testing "Empty paths are considered has non existing"
    (is (nil? (sut/is-existing-file? "")))
    (is (nil? (sut/is-existing-file? nil))))
  (is (not (sut/is-existing-file? "docs")) "A dir is not known has an existing file.")
  (is (sut/is-existing-file? "deps.edn") "Known files are existing")
  (is (nil? (sut/is-existing-file? "non-existing-dir")) "Non existing dir")
  (is (nil? (sut/is-existing-file? "non-existing-file.edn")) "Non existing file"))

(deftest is-existing-dir?-test
  (testing "Empty dirs are considered has non existing"
    (is (nil? (sut/is-existing-dir? "")))
    (is (nil? (sut/is-existing-dir? nil))))
  (is (sut/is-existing-dir? "docs") "Known dir is existing")
  (is (not (sut/is-existing-dir? "deps.edn")) "Known files are existing")
  (is (nil? (sut/is-existing-dir? "non-existing-dir")) "Non existing dir")
  (is (nil? (sut/is-existing-dir? "non-existing-file.edn")) "Non existing file"))

(def root-tmp (sut/create-temp-dir))
(def d (build-filename/create-dir-path root-tmp "test"))
(def d2 (build-filename/create-dir-path root-tmp "test2"))

d

(def expected-files #{"a" "c" "c/d" "c/d/e" "c/d/e/f" "b"})

(def create-file-path (partial build-filename/create-file-path d))

(deftest file-rich-list-test
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
              (mapv #(build-filename/relativize % d))
              set))
      "Test the expected tree")
  (sut/delete-file (create-file-path "a"))
  (is (= #{{:path "b"
            :exists? true
            :file? true}
           {:path "c"
            :exists? true
            :dir? true}
           {:path "c/d"
            :exists? true
            :dir? true}
           {:path "c/d/e"
            :exists? true
            :dir? true}
           {:path "c/d/e/f"
            :exists? true
            :file? true}}
         (->> (sut/search-files d "**" {})
              sut/file-rich-list
              (mapv (fn [file-rich] (update file-rich :path #(build-filename/relativize % d))))
              set))
      "Rich file list contains files/dirs, and nesting."))

(deftest copy-actions
  (is
   (=
    #{{:path "test/c/d"
       :exists? true
       :dir? true
       :src-dir "test"
       :options {:replace-existing true
                 :copy-attributes true}
       :dst-dir "test2"
       :relative-path "c/d"
       :target-dir-path "test2/c/d"}
      {:path "test/b"
       :exists? true
       :file? true
       :src-dir "test"
       :options {:replace-existing true
                 :copy-attributes true}
       :dst-dir "test2"
       :relative-path "b"
       :target-dir-path "test2"}
      {:path "test/c/d/e"
       :exists? true
       :dir? true
       :src-dir "test"
       :options {:replace-existing true
                 :copy-attributes true}
       :dst-dir "test2"
       :relative-path "c/d/e"
       :target-dir-path "test2/c/d/e"}
      {:path "test/c"
       :exists? true
       :dir? true
       :src-dir "test"
       :options {:replace-existing true
                 :copy-attributes true}
       :dst-dir "test2"
       :relative-path "c"
       :target-dir-path "test2/c"}
      {:path "test/c/d/e/f"
       :exists? true
       :file? true
       :src-dir "test"
       :options {:replace-existing true
                 :copy-attributes true}
       :dst-dir "test2"
       :relative-path "c/d/e/f"
       :target-dir-path "test2/c/d/e"}}
    (->> (-> d
             (sut/search-files "**" {})
             sut/file-rich-list
             (sut/copy-actions d d2 {}))
         (mapv (fn [copy-action]
                 (-> copy-action
                     (update :path #(build-filename/relativize % root-tmp))
                     (update :src-dir #(build-filename/relativize % root-tmp))
                     (update :dst-dir #(build-filename/relativize % root-tmp))
                     (update :target-dir-path #(build-filename/relativize % root-tmp)))))
         set))
   "Copy actions discriminate dir and files, and calculate target and relative paths properly.")
  (-> d
      (sut/search-files "**" {})
      sut/file-rich-list
      (sut/copy-actions d d2 {})
      sut/actual-copy)
  (is (= #{"test2/c" "test2/c/d" "test2/c/d/e" "test2/c/d/e/f" "test2/b"}
         (->> (sut/search-files d2 "**" {})
              (mapv #(build-filename/relativize % root-tmp))
              set))
      "After the actual copy, all files are found in `d2`"))

(deftest to-src-dst-test
  (is (= #{["test/c/d" "test2/c/d"] ["test/b" "test2"] ["test/c/d/e" "test2/c/d/e"]
           ["test/c" "test2/c"] ["test/c/d/e/f" "test2/c/d/e"]}
         (->> (-> d
                  (sut/search-files "**" {})
                  sut/file-rich-list
                  (sut/copy-actions d d2 {})
                  sut/to-src-dst)
              (mapv (fn [[src dst]] [(build-filename/relativize src root-tmp)
                                     (build-filename/relativize dst root-tmp)]))
              set))
      "The pairs are ok."))

(deftest read-file-test
  (let [f (sut/read-file "non-existing-file")]
    (is (= {:filename "non-existing-file"
            :invalid? true}
           (dissoc f :exception))
        "Non existing file returns invalid?")
    (is (:exception f) "Non existing files contains errors")))

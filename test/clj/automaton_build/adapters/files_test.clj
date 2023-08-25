(ns automaton-build.adapters.files-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [babashka.fs :as fs]
   [clojure.java.io :as io]

   [automaton-build.adapters.files :as sut]))

(deftest copy-files-or-dir
  (let [tmp-dir (fs/create-temp-dir)]
    (testing "Directory copy"
      (is (= (do
               (sut/copy-files-or-dir [(io/resource "resource-test-copy-dir")]
                                      tmp-dir)
               #{"test1" "test2"})
             (into #{}
                   (map (fn [file]
                          (str (fs/relativize tmp-dir file)))

                        (fs/glob tmp-dir "**")))))))
  (testing "If files are not a vector of string"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"parameter should be a sequence"
                          (sut/copy-files-or-dir {} "/tmp"))))
  (let [tmp-dir (fs/create-temp-dir)]
    (testing "File copy"
      (is (= (do
               (sut/copy-files-or-dir [(io/resource (str "resource-test-copy-dir"
                                                         fs/file-separator
                                                         "test1"))]
                                      tmp-dir)
               #{"test1"})
             (into #{}
                   (map (fn [file]
                          (str (fs/relativize tmp-dir file)))
                        (fs/glob tmp-dir "**"))))))))

(deftest directory-exists?
  (testing "A non existing directory is detected"
    (is (not
         (sut/directory-exists? "non-existing-directory"))))
  (testing "A non existing directory in existing directory is detected"
    (is (not
         (sut/directory-exists? "everything/non-existing"))))
  (testing "A file is detected as a non directory"
    (is (not
         (sut/directory-exists? "deps.edn"))))
  (testing "An existing directory is detected"
    (is (sut/directory-exists? "everything")))
  (testing "An existing directory inside directory is detected"
    (is (sut/directory-exists? "everything/src"))))

(deftest is-existing-file?
  (testing "A non existing path is accepted"
    (is (not (sut/is-existing-file? "non-existing-directory"))))
  (testing "An already existing path is accepted"
    (is (sut/is-existing-file? "deps.edn"))))

(deftest create-temp-dir
  (testing "A temporary directory has really been created"
    (is (sut/directory-exists? (sut/create-temp-dir)))))

(deftest remove-trailing-separator
  (let [base-dir (str sut/file-separator "tmp" sut/file-separator "foo")]
    (testing "Accept directories with no trailing separator"
      (is (= base-dir
             (sut/remove-trailing-separator base-dir))))
    (testing "Remove one trailing separator"
      (is (= base-dir
             (sut/remove-trailing-separator (str base-dir sut/file-separator)))))
    (testing "Remove one trailing separator"
      (is (= base-dir
             (sut/remove-trailing-separator (str base-dir sut/file-separator sut/file-separator)))))
    (testing "Remove one trailing separator"
      (is (= base-dir
             (sut/remove-trailing-separator (str base-dir sut/file-separator " ")))))
    (testing "Remove one trailing separator"
      (is (= base-dir
             (sut/remove-trailing-separator (str " " base-dir sut/file-separator)))))
    (testing "Remove one trailing separator"
      (is (= base-dir
             (sut/remove-trailing-separator (str " " base-dir sut/file-separator " ")))))))

(deftest create-dir-path
  (let [expected-result (str sut/file-separator "tmp" sut/file-separator "foo" sut/file-separator "bar" sut/file-separator)]
    (testing "Creates a simple path"
      (is (= expected-result
             (sut/create-dir-path sut/file-separator "tmp" "foo" "bar"))))
    (testing "Don't add path separator if already there"
      (is (= expected-result
             (sut/create-dir-path (str) sut/file-separator
                                  "tmp" "foo" "bar"))))
    (testing "Empty strings are filtered"
      (is (= expected-result
             (sut/create-dir-path (str) sut/file-separator "tmp" "" "foo" "bar"))))
    (testing "nil path returns nil"
      (is (= "./"
             (sut/create-dir-path))))
    (testing "Trailing separator is not added if already there"
      (is (= expected-result
             (sut/create-dir-path sut/file-separator "tmp" "foo" "bar"))))
    (testing "Relative path are working also"
      (is (= (str "tmp" sut/file-separator "foo" sut/file-separator "bar" sut/file-separator)
             (sut/create-dir-path "tmp" "foo" "bar"))))))

(deftest create-file-path
  (let [expected-result (str sut/file-separator "tmp" sut/file-separator "foo" sut/file-separator "bar")]
    (testing "Creates a simple path"
      (is (= expected-result
             (sut/create-file-path sut/file-separator "tmp" "foo" "bar"))))
    (testing "Don't add path separator if already there"
      (is (= expected-result
             (sut/create-file-path sut/file-separator (str) "tmp" "foo" "bar"))))
    (testing "Empty strings are filtered"
      (is (= expected-result
             (sut/create-file-path sut/file-separator (str) "tmp" "" "foo" "bar"))))
    (testing "No parameters creates a root dir"
      (is (= "."
             (sut/create-file-path))))
    (testing "Trailing separator is not added if already there"
      (is (= expected-result
             (sut/create-file-path sut/file-separator "tmp" "foo" "bar"))))
    (testing "Relative path are working also"
      (is (= (str "tmp" sut/file-separator "foo" sut/file-separator "bar")
             (sut/create-file-path "tmp" "foo" "bar"))))))

(deftest change-extension
  (testing "Change the extension"
    (is (= "toto.edn"
           (sut/change-extension "toto.tmp" ".edn")))
    (is (= "toto.edn.edn"
           (sut/change-extension "toto.edn.tmp" ".edn")))
    (is (= "toto.edn"
           (sut/change-extension "toto" ".edn")))))

(deftest create-dirs
  (testing "Testing an existing file"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Can't create a directory"
                          (sut/create-dirs "deps.edn"))))
  (testing "Testing a possible directory"
    (is (sut/create-dirs (sut/create-temp-dir)))))

(deftest absolutize
  (testing "Absolute don't change an absolute path"
    (is (= "/foo"
           (sut/absolutize "/foo"))))
  (testing "Absolute changes a relative path"
    (is (not= "foo/bar"
              (sut/absolutize "foo/bar"))))
  (testing "Nil is not failing"
    (is (nil?
         (sut/absolutize nil)))))

(deftest file-name
  (testing "Extract file name"
    (is (= "baz"
           (sut/file-name "/foo/bar/baz")))))

(deftest file-in-same-dir
  (testing "Empty root directory is ok"
    (is (= "foo"
           (sut/file-in-same-dir "" "foo"))))
  (testing "If the source is directory, the files are stored in it"
    (is (= "automaton/automaton-web/src/clj/foo"
           (sut/file-in-same-dir "automaton/automaton-web/src/clj" "foo"))))
  (testing "If the source is file, stored in the same parent"
    (is (= "automaton/automaton-web/foo"
           (sut/file-in-same-dir "automaton/automaton-web/deps.edn" "foo"))))
  (testing "If the source is file does not exist, store in the same parent"
    (is (= "automaton/automaton-web/foo"
           (sut/file-in-same-dir "automaton/automaton-web/deps-does-not-exist.edn" "foo")))))

(deftest file-ized-test
  (testing ""
    (is (= "foo_bar"
           (sut/file-ized "foo-bar")))))

(deftest add-suffix-test
  (testing ""
    (is (= "core.mustache.clj"
           (sut/add-suffix "core.clj" ".mustache")))))

(deftest extract-path-test
  (testing "A relative file returns nil"
    (is (= "./"
           (sut/extract-path "README.md"))))
  (testing "A relative file returns nil"
    (is (= "everything/outdated/"
           (sut/extract-path "everything/outdated/deps.edn"))))
  (testing "An absolute file returns nil"
    (is (= "/usr/bin/"
           (sut/extract-path "/usr/bin/ls"))))
  (testing "Extract a path from a file"
    (is (= "/foo/"
           (sut/extract-path "/foo/bar")))
    (is (= "/foo/bar/foo2/"
           (sut/extract-path "/foo/bar/foo2/bar2")))))

(deftest filter-existing-dir-test
  (testing "Only existing dir are filtered"
    (is (= 2
           (count
            (sut/filter-existing-dir ["automaton/automaton-web" "template_app" "non-existing-dir-filtered"]))))))

(deftest empty-path?
  (testing "Empty path"
    (is (sut/empty-path? (sut/create-temp-dir)))))

(comment
  (keys
   (sut/create-files-map "landing"
                         "**{.clj,.cljs,.cljc,.edn}"))
  ;
  )

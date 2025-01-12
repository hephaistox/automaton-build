(ns automaton-build.headers.files-test
  (:require
   [automaton-build.echo.headers  :refer [build-writter]]
   [automaton-build.headers.files :as sut]
   [automaton-build.os.file       :as build-file]
   [automaton-build.os.filename   :as build-filename]
   [clojure.java.io               :as io]
   [clojure.string                :as str]
   [clojure.test                  :refer [deftest is]]))

;; ********************************************************************************
;; File reading

(deftest read-file-quiet-test
  (is (-> "project.edn"
          sut/read-file-quiet
          :raw-content
          string?))
  (is (-> "non-existing"
          sut/read-file-quiet
          :invalid?)))

(deftest read-file-if-error-test
  (is (-> "project.edn"
          sut/read-file-if-error
          :raw-content
          string?)
      "Returns file desc")
  (is (= ""
         (with-out-str (-> "project.edn"
                           sut/read-file-if-error)))
      "A successful file loading prints nothing")
  (is (not (str/blank? (with-out-str (-> "non-existing"
                                         sut/read-file-if-error))))
      "A successful file loading prints nothing"))

(deftest read-file-test
  (is (not (str/blank? (with-out-str (-> "project.edn"
                                         sut/read-file
                                         :raw-content
                                         string?))))
      "File loading is announced"))

;; ********************************************************************************
;; edn reading

(deftest read-edn-quiet-test
  (is (-> "project.edn"
          sut/read-edn-quiet
          :raw-content
          string?))
  (is (-> "non-existing"
          sut/read-edn-quiet
          :invalid?)))

(deftest read-edn-if-error-test
  (is (-> "project.edn"
          sut/read-edn-if-error
          :raw-content
          string?)
      "Returns file desc")
  (is (= ""
         (with-out-str (-> "project.edn"
                           sut/read-edn-if-error)))
      "A successful file loading prints nothing")
  (is (not (str/blank? (with-out-str (-> "non-existing"
                                         sut/read-edn-if-error))))
      "A successful file loading prints nothing"))

(deftest read-edn-test
  (is (not (str/blank? (with-out-str (-> "project.edn"
                                         sut/read-edn
                                         :raw-content
                                         string?))))
      "File loading is announced"))

;; ********************************************************************************
;; project configuration

(deftest project-config-test (is (sut/project-config "")))

;; ********************************************************************************
;; search, move and copy files
;;
(deftest create-sym-link-quiet-test
  (let [tmp-dir (build-file/create-temp-dir "test")
        src-dir (build-filename/create-file-path tmp-dir "foo")
        dst-dir (build-filename/create-dir-path tmp-dir "foo2")
        src-file (build-filename/create-file-path src-dir "bar.edn")
        dst-file (build-filename/create-file-path dst-dir "symlink")]
    (build-file/ensure-dir-exists src-dir)
    (build-file/ensure-dir-exists dst-dir)
    (build-file/write-file src-file "Testing sym link")
    (is (= :success (:status (sut/create-sym-link-quiet src-file dst-file tmp-dir)))
        "Is the creation sucessful in a new directory")
    (is (= :failure (:status (sut/create-sym-link-quiet src-file dst-file tmp-dir)))
        "Is the second creation failing")
    (is (= "Testing sym link" (:raw-content (build-file/read-file dst-file))))))

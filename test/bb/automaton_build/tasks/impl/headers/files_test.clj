(ns automaton-build.tasks.impl.headers.files-test
  (:require
   [automaton-build.echo.headers             :refer [build-writter]]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.filename              :as build-filename]
   [automaton-build.tasks.impl.headers.files :as sut]
   [clojure.string                           :as str]
   [clojure.test                             :refer [deftest is]]))

(deftest read-file-test
  (is (string? (sut/read-file "package.json")) "package.json exists and contains a string.")
  (is (str/blank? (with-out-str (sut/read-file "package.json")))
      "package.json reading doesn't creates any log.")
  (is (str/blank? (with-out-str (sut/read-file "package.json")))
      "package.json reading doesn't creates any log.")
  (is (not (str/blank? (with-out-str (sut/read-file "non-existing-file"))))
      "Reading a non existing file creates log."))

(deftest read-edn-test
  (is (some? (sut/read-edn-file "deps.edn")) "Reading exists and contains data.")
  (is (str/blank? (with-out-str (sut/read-edn-file "deps.edn")))
      "Reading of `deps.edn` doesn't creates any log.")
  (is (str/blank? (with-out-str (sut/read-edn-file "deps.edn")))
      "Package.json reading doesn't creates any log.")
  (is (not (str/blank? (with-out-str (sut/read-edn-file "non-existing-file.edn"))))
      "Reading a non existing file creates log."))

(deftest copy-files-test
  (let [tmp-dir (build-file/create-temp-dir "copy-files-test")]
    (sut/copy-files "docs" tmp-dir "*" false {})
    (is (= #{"archi" "dev_rules.md" "tutorial" "customer_materials" "ADR" "reports" "code"}
           (->> (build-file/search-files tmp-dir "*")
                (mapv #(build-filename/relativize % tmp-dir))
                set))
        "Check copy has happened.")))

(deftest create-sym-link-test
  (let [tmp-dir (build-file/create-temp-dir "test")
        src-dir (build-filename/create-file-path tmp-dir "foo")
        dst-dir (build-filename/create-dir-path tmp-dir "foo2")
        src-file (build-filename/create-file-path src-dir "bar.edn")
        dst-file (build-filename/create-file-path dst-dir "symlink")]
    (build-file/ensure-dir-exists src-dir)
    (build-file/ensure-dir-exists dst-dir)
    (build-file/write-file src-file "Testing sym link")
    (is (sut/create-sym-link src-file dst-file tmp-dir)
        "Is the creation sucessful in a new directory")
    (is (not (binding [*out* (build-writter)] (sut/create-sym-link src-file dst-file tmp-dir)))
        "Is the second creation failing")
    (is (= "Testing sym link" (:raw-content (build-file/read-file dst-file))))
    (is (re-find #"The symlink should have a directory as a base-dir"
                 (with-out-str (:raw-content (build-file/read-file
                                              (sut/create-sym-link src-file dst-file src-file)))))
        "If `base-dir` is a file, a message is printed")))

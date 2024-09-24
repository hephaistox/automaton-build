(ns automaton-build.os.edn-utils-test
  (:require
   [automaton-build.os.edn-utils :as sut]
   [automaton-build.os.file      :as build-file]
   [clojure.test                 :refer [deftest is]]))

(deftest read-edn-test
  (is (= {:filename "non-existing-file"
          :invalid? true}
         (dissoc (sut/read-edn "non-existing-file") :exception))
      "Non existing file is skipped.")
  (is (and (build-file/is-existing-file? "README.md") (:invalid? (sut/read-edn "README.md")))
      "Non edn file is skipped")
  (is (= [:filename :dir :raw-content :edn] (keys (sut/read-edn "deps.edn"))))
  (is (= [:filename :dir :raw-content :exception :invalid?] (keys (sut/read-edn "README.md"))))
  (is (= [:filename :exception :invalid?] (keys (sut/read-edn "non-existing-file")))))

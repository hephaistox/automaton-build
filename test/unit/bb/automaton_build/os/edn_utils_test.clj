(ns automaton-build.os.edn-utils-test
  (:require
   [automaton-build.os.edn-utils :as sut]
   [automaton-build.os.file      :as build-file]
   [clojure.test                 :refer [deftest is]]))

(deftest read-edn-test
  (is (= {:filename "non-existing-file"
          :exception true
          :invalid? true}
         (update (sut/read-edn "non-existing-file") :exception some?))
      "Non existing file")
  (is (and (build-file/is-existing-file? "README.org") (:invalid? (sut/read-edn "README.org")))
      "Non edn file is skipped")
  (is (= #{:filename :dir :raw-content :exception :invalid?}
         (set (keys (sut/read-edn "README.org"))))
      "Non edn file returns exception and invalid?")
  (is (= #{:filename :dir :raw-content :edn} (set (keys (sut/read-edn "deps.edn"))))
      "Existing edn file")
  (is (= #{:filename :exception :invalid?} (set (keys (sut/read-edn "non-existing-file"))))
      "Non existing file"))

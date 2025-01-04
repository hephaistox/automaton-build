(ns automaton-build.code.files-test
  (:require
   [automaton-build.code.files   :as sut]
   [automaton-build.os.edn-utils :as build-edn]
   [clojure.test                 :refer [deftest is]]))

(deftest project-dirs-test
  (is (empty? (sut/project-dirs "" {})) "If paths is empty, no dir is returned.")
  (is (= #{"test/bb/"} (sut/project-dirs "" {:paths ["test/bb"]})) "Existing path is kept")
  (is (= #{"test/bb/"}
         (sut/project-dirs "" {:paths ["non-existing-dir" "test/bb" "env/non-exist/src"]}))
      "Non existing dirs are skipped")
  (is (= #{"test/bb/"}
         (sut/project-dirs ""
                           {:paths ["test/bb"]
                            :aliases {:foo {:extra-paths ["test/bb"]}}}))
      "If the same path is at many places, it is returned only once.")
  (is (seq (apply sut/project-dirs ((juxt :dir :edn) (build-edn/read-edn "deps.edn"))))
      "A project in a sub directory manages properly the paths."))

(deftest project-files-test
  (is (< 20
         (-> (sut/project-dirs "" (:edn (build-edn/read-edn "deps.edn")))
             sut/project-files
             count))))

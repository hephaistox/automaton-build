(ns automaton-build.code.files-test
  (:require
   [automaton-build.code.files      :as sut]
   [automaton-build.os.edn-utils-bb :as build-edn]
   [clojure.test                    :refer [deftest is]]))

(deftest project-dirs-test
  (is (empty? (sut/project-dirs "" {})) "If no path is provided, not dir is.")
  (is (= #{"env/test/src/"}
         (sut/project-dirs "" {:paths ["non-existing-dir" "env/test/src" "env/non-exist/src"]}))
      "Values in `:paths` are returned")
  (is (= #{"env/test/src/"}
         (sut/project-dirs ""
                           {:paths ["env/test/src"]
                            :aliases {:foo {:extra-paths ["env/test/src"]}}}))
      "If the same path is at many places, it is returned only once.")
  (is (seq (apply sut/project-dirs ((juxt :dir :edn) (build-edn/read-edn "deps.edn"))))
      "A project in a sub directory manages properly the paths."))

(deftest project-files-test
  (is (< 20
         (-> (sut/project-dirs "" (:edn (build-edn/read-edn "deps.edn")))
             sut/project-files
             count))))

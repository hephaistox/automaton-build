(ns automaton-build.code.files-test
  (:require
   [automaton-build.code.files      :as sut]
   [automaton-build.os.edn-utils-bb :as build-edn]
   [clojure.test                    :refer [deftest is]]))

(deftest project-dirs-test
  (is (empty? (sut/project-dirs {:dir ""
                                 :edn {}}))
      "If no path is provided, not dir is.")
  (is (= #{"env/test/src/"}
         (sut/project-dirs {:dir ""
                            :edn {:paths ["non-existing-dir"
                                          "env/test/src"
                                          "env/non-exist/src"]}}))
      "Values in `:paths` are returned")
  (is (= #{"env/test/src/"}
         (sut/project-dirs {:dir ""
                            :edn {:paths ["env/test/src"]
                                  :aliases {:foo {:extra-paths
                                                  ["env/test/src"]}}}}))
      "If the same path is at many places, it is returned only once.")
  (is (seq (sut/project-dirs (build-edn/read-edn
                              "automaton/automaton_build/deps.edn")))
      "A project in a sub directory manages properly the paths."))

(deftest project-files-test
  (is (< 20
         (-> ["env/test/src" "automaton/automaton_build/src/clj"]
             sut/project-files
             count))))

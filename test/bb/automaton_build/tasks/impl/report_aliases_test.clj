(ns automaton-build.tasks.impl.report-aliases-test
  (:require
   [automaton-build.os.edn-utils-bb           :as build-edn]
   [automaton-build.tasks.impl.report-aliases :as sut]
   [clojure.test                              :refer [deftest is]]))

(deftest project-files-test
  (is (< 100
         (count (-> (build-edn/read-edn "deps.edn")
                    sut/project-files)))))

(deftest alias-test
  (is (< 1000
         (count (-> (build-edn/read-edn "deps.edn")
                    sut/project-files
                    sut/alias-list)))))

(deftest scan-alias-project-test
  (is (do (with-out-str (sut/scan-alias-project* (-> (build-edn/read-edn
                                                      "deps.edn")
                                                     sut/project-files)
                                                 false))
          true)
      "The alias scan is done without exception."))

(deftest scan-alias-test
  (is (do (with-out-str (sut/scan-alias {:subprojects [{:deps
                                                        (build-edn/read-edn
                                                         "deps.edn")}]}
                                        true))
          true)))

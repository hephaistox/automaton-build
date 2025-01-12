(ns automaton-build.headers.report-aliases-test
  (:require
   [automaton-build.headers.report-aliases :as sut]
   [automaton-build.os.edn-utils           :as build-edn]
   [clojure.test                           :refer [deftest is]]))

;; ********************************************************************************
;; Project directories
;; ********************************************************************************
(deftest project-files-test
  (is (= 7
         (count (-> (build-edn/read-edn "deps.edn")
                    sut/project-dirs)))))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(deftest alias-test
  (is (< 100
         (count (-> (build-edn/read-edn "deps.edn")
                    sut/project-dirs
                    sut/project-files
                    sut/alias-list)))))

(deftest scan-alias-project-test
  (is (do (with-out-str (sut/scan-alias-project* (-> (build-edn/read-edn "deps.edn")
                                                     sut/project-dirs
                                                     sut/project-files)
                                                 true))
          true)
      "The alias scan is done without exception."))

(deftest scan-alias-test
  (is (do (with-out-str (sut/scan-alias {:subprojects [{:deps (-> "deps.edn"
                                                                  build-edn/read-edn
                                                                  sut/project-dirs)}]}
                                        true))
          true)))

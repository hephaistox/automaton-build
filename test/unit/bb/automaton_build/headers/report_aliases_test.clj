(ns automaton-build.headers.report-aliases-test
  (:require
   [automaton-build.headers.report-aliases :as sut]
   [automaton-build.os.edn-utils           :as build-edn2]
   [clojure.test                           :refer [deftest is]]))

(defn- noop [& _] nil)

(def noop-printers
  {:title noop
   :title-valid! noop
   :title-error! noop
   :subtitle noop
   :uri-str noop
   :normalln noop})
;; ********************************************************************************
;; Project directories
;; ********************************************************************************
(deftest project-files-test
  (is (= 7
         (count (-> (build-edn2/read-edn "deps.edn")
                    sut/project-dirs)))))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(deftest alias-test
  (is (< 100
         (count (-> (build-edn2/read-edn "deps.edn")
                    sut/project-dirs
                    sut/project-files
                    sut/alias-list)))))

(deftest scan-alias-project-test
  ;;TODO Check what files are scanned
  (is (sut/scan-alias-project noop-printers
                              (-> (build-edn2/read-edn noop-printers "deps.edn")
                                  sut/project-dirs
                                  sut/project-files)
                              true)
      "The alias scan is done without exception."))

(deftest scan-alias-test
  (is (sut/scan-alias noop-printers
                      {:subprojects [{:deps (-> (build-edn2/read-edn noop-printers "deps.edn")
                                                sut/project-dirs)}]}
                      true)))

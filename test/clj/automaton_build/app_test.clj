(ns automaton-build.app-test
  (:require
   [automaton-build.app :as sut]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files :as build-files]
   [clojure.test :refer [deftest is testing]]))

(deftest update-app-dep-test
  (let [dir (build-files/create-temp-dir)
        deps-edn (build-files/create-file-path dir "deps.edn")
        build-config (build-files/create-file-path dir "build_config.edn")
        nested-deps-edn (build-files/create-file-path dir "deep/deps.edn")
        nested-build-config
        (build-files/create-file-path dir "deep/build_config.edn")
        _project (build-files/spit-file build-config {})
        _project-deps (build-files/spit-file deps-edn
                                             {:deps {:a "2"
                                                     :b "3"}
                                              :aliases {:build {:extra-deps
                                                                {:a "2"
                                                                 :c "2"}}}})
        _nested-project (build-files/spit-file nested-build-config {})
        _nested-project-deps (build-files/spit-file
                              nested-deps-edn
                              {:deps {:a "2"}
                               :aliases {:build {:extra-deps {:a "2"}}}})]
    (testing "App deps.edn updated"
      (is (every? true? (vals (sut/update-app-dep dir :a "5"))))
      (is (= {:deps {:a "5"
                     :b "3"}
              :aliases {:build {:extra-deps {:a "5"
                                             :c "2"}}}}
             (build-edn-utils/read-edn deps-edn)))
      (is (= {:deps {:a "5"}
              :aliases {:build {:extra-deps {:a "5"}}}}
             (build-edn-utils/read-edn nested-deps-edn))))))

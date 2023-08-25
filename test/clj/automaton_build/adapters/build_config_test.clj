(ns automaton-build.adapters.build-config-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.build-config :as sut]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.edn-utils :as edn-utils]))

(deftest search-for-build-config-test
  (testing "Are projects found"
    (is (> (count (sut/search-for-build-config))
           0))))

(deftest spit-build-config-test
  (testing "Check spitted build config is found"
    (let [tmp-dir (files/create-temp-dir)
          content {:foo3 :bar3}]
      (sut/spit-build-config tmp-dir
                             content
                             ";; Hey!")
      (is (= content
             (edn-utils/read-edn-or-nil (files/create-file-path tmp-dir
                                            sut/build-config-filename)))))))

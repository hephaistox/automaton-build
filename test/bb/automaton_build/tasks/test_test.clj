(ns automaton-build.tasks.test-test
  (:require
   [automaton-build.tasks.test :as sut]
   [clojure.test               :refer [deftest is]]))

(def test-definitions
  #{{:description "test for la"
     :alias :la}
    {:description "test for unit"
     :alias :unit}
    {:description "dev test"
     :alias :dev}})

(deftest test-command-test
  (is (= [{:cmd ["clojure" "-M:foo:bb"]
           :alias :bb}
          {:cmd ["clojure" "-M:foo:a"]
           :alias :a}]
         (sut/test-command "foo" #{{:alias :a} {:alias :bb}}))))

(deftest alias-in-deps-edn-test (is (sut/alias-in-deps-edn)))

(deftest validate-definitions-test
  (is (= {:alias-doesnt-exist #{{:description "test for la"
                                 :alias :la}}
          :alias-exist #{{:description "test for unit"
                          :alias :unit}
                         {:description "dev test"
                          :alias :dev}}}
         (-> (sut/validate-definitions test-definitions #{:unit :dev})
             (update-vals set)))))

(deftest select-test
  (is (= {:selected (set (mapv :alias test-definitions))
          :test-definitions test-definitions
          :non-existing #{}
          :filtered test-definitions}
         (sut/select test-definitions [:all]))
      "All selected")
  (is (= {:test-definitions test-definitions
          :filtered #{{:description "test for la"
                       :alias :la}
                      {:description "test for unit"
                       :alias :unit}}
          :non-existing #{}
          :selected #{:la :unit}}
         (sut/select test-definitions [:la :unit]))
      "Two selected")
  (is (= {:filtered #{{:description "test for la"
                       :alias :la}
                      {:description "test for unit"
                       :alias :unit}}
          :test-definitions test-definitions
          :non-existing #{"non-existing"}
          :selected #{:la :unit}}
         (sut/select test-definitions [:la :unit "non-existing"]))
      "A non existing selected"))

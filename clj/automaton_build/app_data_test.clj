(ns automaton-build.app-data-test
  (:require
   [automaton-build.app-data :as sut]
   [clojure.test :refer [deftest is testing]]))

(def build-app-stub
  "Application after the loading"
  {:build-config {}
   :deps-edn {:paths ["src/clj/"]
              :aliases {:foo {:extra-paths ["src/cljc/"]}
                        :cljs-paths {:extra-paths ["src/cljc/" "src/cljs/"]}}}
   :shadow-cljs {}})

(deftest classpath-dirs-test
  (testing "Returns some existing current source dir of the current app dir"
    (is (= 3 (count (sut/classpath-dirs build-app-stub))))))

(ns automaton-build.app-data.impl-test
  (:require
   [automaton-build.app-data.impl :as sut]
   [clojure.test                  :refer [deftest is testing]]))

(def app-stub
  {:build-config {}
   :deps-edn {:paths ["src/clj"]
              :aliases {:foo {:extra-paths ["src/cljc"]}
                        :cljs-paths {:extra-paths ["src/cljc" "src/cljs"]}}}
   :shadow-cljs {}})

(deftest get-clj-c-src-dirs-test
  (testing "Returns some existing current source dir of the current app dir"
    (is (= 3 (count (sut/clj-compiler-classpath app-stub))))))

(deftest get-cljc-s-src-dirs-test
  (testing "automaton-buil-app has no cljc or cljs files"
    (is (empty? (sut/cljs-compiler-classpaths app-stub)))))

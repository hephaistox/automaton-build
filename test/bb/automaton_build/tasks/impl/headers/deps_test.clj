(ns automaton-build.tasks.impl.headers.deps-test
  (:require
   [automaton-build.echo.headers            :refer [build-writter]]
   [automaton-build.tasks.impl.headers.deps :as sut]
   [clojure.test                            :refer [deftest is]]))

(deftest deps-edn-test
  (is (map? (sut/deps-edn "")) "An existing `deps.edn` is returning a map.")
  (is (= "[31m    No valid `deps.edn` found in directory  `non-existing-dir` \n[39m"
         (with-out-str (sut/deps-edn "non-existing-dir")))
      "A non existing file is displayed.")
  (is (nil? (binding [*out* (build-writter)] (sut/deps-edn "non-existing-dir")))
      "A non existing file returns `nil`."))

(ns automaton-build.tasks.impl.headers.deps-test
  (:require
   [automaton-build.echo.headers            :refer [build-writter]]
   [automaton-build.tasks.impl.headers.deps :as sut]
   [clojure.string                          :as str]
   [clojure.test                            :refer [deftest is]]))

(deftest deps-edn-test
  (is (map? (sut/deps-edn "")) "An existing `deps.edn` is returning a map.")
  (is (str/includes? (with-out-str (sut/deps-edn "non-existing-dir")) "No valid")
      "A non existing file is displayed.")
  (is (nil? (binding [*out* (build-writter)] (sut/deps-edn "non-existing-dir")))
      "A non existing file returns `nil`."))

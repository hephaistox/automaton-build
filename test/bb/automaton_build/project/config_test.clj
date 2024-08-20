(ns automaton-build.project.config-test
  (:require
   [automaton-build.project.config :as sut]
   [clojure.test                   :refer [deftest is]]))

(deftest filename-test
  (is (= "project.edn" (sut/filename ""))
      "\"\" is understoord like current dir")
  (is (= "project.edn" (sut/filename nil))
      "nil is understoord like current dir")
  (is (= "./project.edn" (sut/filename "."))
      "nil is understoord like current dir")
  (is (= "foo/project.edn" (sut/filename "foo"))
      "nil is understoord like current dir"))


(deftest read-from-dir-test
  (is (= [:filename :dir :raw-content :edn] (keys (sut/read-from-dir "")))
      "Is a file descriptor."))

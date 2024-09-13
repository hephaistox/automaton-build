(ns automaton-build.project.impl.gh-yml-test
  (:require
   [automaton-build.project.impl.gh-yml :as sut]
   [clojure.test                        :refer [deftest is]]))

(deftest update-gha-version
  (is
   (=
    "\n        uses: docker://hephaistox/foobar:1.2.3\n        uses: docker://hephaistox/barfoo:2.0.4\n"
    (sut/update-gha-version
     {:raw-content
      "\n        uses: docker://hephaistox/foobar:2.0.3\n        uses: docker://hephaistox/barfoo:2.0.4\n"}
     "foobar"
     "1.2.3"))
   "The foobar dependency's version is replaced, not the other one"))

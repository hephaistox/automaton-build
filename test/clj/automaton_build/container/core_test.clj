(ns automaton-build.container.core-test
  (:require [automaton-build.container.core :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest container-images-container-names-test
  (testing "Container images are found"
    (is (>= 2
            (count (sut/container-image-container-names))))))

(deftest is-container-image?-test
  (testing "A listed container image is authorized"
    (is (sut/is-container-image? "cc-image")))
  (testing "A non listed container image is not authorized"
    (is (not (sut/is-container-image? "not-found-container-image")))))

(ns automaton-build.headers.cmd-test
  (:require
   [automaton-build.headers.cmd :as sut]
   [clojure.test                :refer [deftest is]]))

;; ********************************************************************************
;; Low level API
;; ********************************************************************************

(deftest muted-test (is (= "" (with-out-str (sut/muted ["ls" "-la"] "")))))

(comment
  (sut/printing ["ls" "-la"] "" 1)
  (sut/print-on-error ["ls" "aze"] "" 1 100 100)
  ;
)

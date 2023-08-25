(ns automaton-build.apps.code-publication-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.apps.code-publication :as sut]
   [automaton-build.apps :as apps]))

(def lib
  (apps/search-app-by-name bafaaft/apps
                           "automaton-core"))

(comment
  (sut/push-a-lib lib
                  bafaaft/apps
                  "Test")
;
  )

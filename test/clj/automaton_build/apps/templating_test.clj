(ns automaton-build.apps.templating-test
  (:require
   [automaton-build.apps.templating :as sut]
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.apps :as apps]))

(def apps
  bafaaft/apps)

(comment
  (let [template-app (apps/template-app apps)]
    (apps/first-app-matching bafaaft/apps
                             #(sut/refresh-project %
                                                   template-app
                                                   true)))
  (sut/change-markers "")

  [{:file ["base" "automaton"]
    :ns ["base" "automaton"]}
   {:file ["endpoint/realtime" "duplex"]
    :ns ["endpoint.realtime" "duplex"]}
   {:file ["base/server" "duplex"]
    :ns ["endpoint.realtime" "duplex"]}]

  (sut/rename-dirs apps
                   {"base" [{:file ["base" "automaton"]
                             :ns ["base" "automaton"]}]})

  (sut/search-pattern apps
                      "base/server")
;
  )

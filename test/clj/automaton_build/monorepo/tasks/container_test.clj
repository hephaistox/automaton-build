(ns automaton-build.monorepo.tasks.container-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.monorepo.tasks.container :as sut]))

(def apps
  bafaaft/apps)

(def cust-app
  (first (filter :cust-app?
                 apps)))

(comment
  (sut/lconnect apps
                {:cust-app-name (:app-name cust-app)})

  (sut/gha-connect apps
                   {})
;
  )

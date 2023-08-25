(ns automaton-build.monorepo.doc
  (:require
   [automaton-build.adapters.doc :as doc]
   [automaton-build.apps :as apps]))

(defn build-doc
  "Generate the documentation for the application `app`
  * `apps` is the applications to build doc from"
  [apps]
  (doc/build-doc "Monorepo doc"
                 "Monorepo doc"
                 "Monorepo doc"
                 ".."
                 (apps/get-existing-src-dirs apps)))

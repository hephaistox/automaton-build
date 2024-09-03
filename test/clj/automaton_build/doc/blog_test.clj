(ns automaton-build.doc.blog-test
  (:require
   [automaton-build.doc.blog :as sut]))

(comment
  (sut/configuration-data "../../../docs/customer_materials/elevator/elevator.edn"
                          "../../../tmp/html"
                          "../../../tmp/pdf")
  (-> (sut/configuration-data "../../../docs/customer_materials/elevator/elevator.edn"
                              "../../../tmp/html"
                              "../../../tmp/pdf")
      first
      sut/configuration-data-by-language-to-html-pdf)
  ;
)

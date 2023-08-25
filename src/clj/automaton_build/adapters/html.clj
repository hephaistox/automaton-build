(ns automaton-build.adapters.html
  "Manipulate html"
  (:require
   [automaton-core.adapters.files :as files]

   [hiccup2.core :as h.page]
   [markdown.core :refer [md-to-html]]
   [automaton-core.env-setup :as env-setup]))

(defn- hephaistox-logo
  "Hephaistox logo clj-pdf element created for header"
  [src header-id]
  [:img
   {:id header-id
    :src src}])

(defn header-str
  [{:keys [logo-path
           header-id]}]
  (str (h.page/html (hephaistox-logo logo-path
                                     header-id))))

(defn md->html-str
  [{:keys [md-path
           header-id]}]
  (let [tmp-dir (get-in env-setup/env-setup [:customer-materials :tmp-html-dir])
        _ (files/create-dirs tmp-dir)
        tmp-html-filename (files/create-file-path tmp-dir
                                         "test.html")
        _create-html-file (md-to-html md-path
                                      tmp-html-filename)

        html-file-str (files/read-file tmp-html-filename)

        header (when header-id
                 (header-str {:logo-path "logo.jpg"
                              :header-id header-id}))]
    (str header
         html-file-str)))

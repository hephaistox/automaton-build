(ns automaton-build.blog
  "All the functions related to blogging/creating content"
  (:require
   [automaton-build.adapters.html :as bah]
   [automaton-build.adapters.pdf :as bap]
   [automaton-build.adapters.files :as baf]
   [automaton-build.adapters.log :as bal]))

(defn blog-md->pdf
  "Creates pdf file from md document with pdf document meta data and hephaistox footer/header on every page.
   * document-name - string displayed document name in the footer
   * pdf-metadata - map with metadata to include into the pdf file
   * file-path - place where markdown file can be found
   * resources-dir - directory where you can find resources that the md file is referencing"
  [{:keys [document-name
           pdf-metadata
           md-path
           pdf-path
           resources-dir]}]

  (let [header-id "margin-header"
        header&footer {:margin-box  {:top-left    {:element header-id}
                                     :bottom-left {:text (str "Hephaistox - " document-name)}
                                     :bottom-right {:paging [:page " of " :pages]}}}

        md-path (str md-path ".md")

        pdf-path (str pdf-path ".pdf")

        html-str (bah/md->html-str {:md-path md-path
                                      :header-id header-id})

        styles (-> (str resources-dir "blog.css")
                   baf/absolutize
                   baf/file-prefix)]
    (bap/html-str->pdf
     {:html-str html-str
      :output-path pdf-path
      :resources-dir resources-dir
      :pdf-metadata pdf-metadata
      :margin-box header&footer
      :styles styles})

    (bal/trace (format "File generated from: `%s` to `%s`" md-path pdf-path))))

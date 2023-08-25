(ns automaton-build.adapters.pdf
  "Manipulate pdf"
  (:require
   [clj-htmltopdf.core :refer [->pdf]]
   [clojure.string :as cs]
   [automaton-core.adapters.files :as baf]))

(defn- img-src
  "full html img tag src, containing both a key and a value as a string"
  [path]
  (str "src=\"" path "\""))

(defn- url?
  "Checks if string is a url. True if it is."
  [path]
  (= "http" (cs/join "" (take 4 path))))

(defn src->accepted-src
  "Converts img src files references to ones that are accepted by clj-htmltopdf library.
   You can read more here: https://github.com/gered/clj-htmltopdf#file-path--url-resolving"
  [resources-dir html-str]
  (cs/replace html-str
              #"src=\"(.*?)\""
              (fn [[full-match path]]
                (if (url? path)
                  full-match
                  (img-src
                   (-> (str resources-dir path)
                       baf/absolutize
                       baf/file-prefix))))))

(defn html-str->pdf
  "Converts html string to pdf file.
   * html-str -> string with html to convert
   * output-path -> path where pdf should be generated
   * pdf-metadata -> map with pdf metadata related keys and values
   * margin-box -> map containing definitions for headers/footers
                   keys are defined here https://www.w3.org/TR/css-page-3/#margin-boxes
   * styles -> a styles to use when converting html to pdf, values defined here https://github.com/gered/clj-htmltopdf#styles"
  [{:keys [html-str
           output-path
           resources-dir
           pdf-metadata
           margin-box
           styles]}]
  (let [updated-img-html (src->accepted-src resources-dir html-str)]
    (->pdf updated-img-html
           output-path
           {:doc pdf-metadata
            :page margin-box
            :styles styles})))

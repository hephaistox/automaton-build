(ns automaton-build.doc.markdown-to-html
  "Markdown adapter to generate html"
  (:require
   [markdown.core :as markdown]))

(defn md-to-html
  "Transform md file to html"
  [md-filename html-filename]
  (markdown/md-to-html md-filename html-filename))

(ns automaton-build.doc.mermaid-bb
  "To create mermaid images
  Proxy to cli mermaid"
  (:require
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.filename :as build-filename]))

(def ^:private mermaid-pattern "**.mermaid")

(defn need-to-update?
  "Returns true if `file-in` needed to be modified, compared to `file-out`."
  [file-in file-out]
  (and (build-filename/match-extension? file-in ".mermaid")
       (seq (build-file/modified-since file-out [file-in]))))

(defn build-mermaid-image-cmd
  "Command to update `mermaid-filename` to an image with extension `image-extension`."
  [mermaid-filename image-extension]
  ["node_modules/.bin/mmdc"
   "-i"
   mermaid-filename
   "-o"
   (build-filename/change-extension mermaid-filename image-extension)])

(defn ls-mermaid
  "List all mermaid files searched recursively in `app-dir`."
  [app-dir]
  (->> (build-file/matching-files app-dir mermaid-pattern)
       (mapv str)))

(defn files-to-recompile
  "List files that need to be recompiled, i.e. the one existing and which image is older than the last update of the source."
  [mermaid-files image-extension]
  (->> mermaid-files
       (mapv str)
       (filterv (fn [file]
                  (let [file-out
                        (build-filename/change-extension file image-extension)]
                    (and (build-file/is-existing-file? file)
                         (need-to-update? file file-out)))))))

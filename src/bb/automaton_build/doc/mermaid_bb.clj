(ns automaton-build.doc.mermaid-bb
  "Create mermaid images.

  Proxy to [mermaid cli](https://www.npmjs.com/package/@mermaid-js/mermaid-cli)"
  (:require
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.filename :as build-filename]))

(def ^:private mermaid-pattern "**.mermaid")

(defn mermaid-pull-cli-cmd
  "Command to install the mermaid cli."
  []
  ["docker" "pull" "minlag/mermaid-cli"])

(defn mermaid-build-image-cmd
  "Command to transform `mermaid-filepath` into an image -a png- into the `target-dir`.

  `user-id` and `group-id` are necessary to execute the command. "
  [mermaid-filepath user-id group-id target-dir]
  (let [mermaid-filename (build-filename/filename mermaid-filepath)
        image-filename (build-filename/change-extension mermaid-filename ".png")]
    {:output-file (build-filename/create-file-path target-dir image-filename)
     :cmd ["docker"
           "run"
           "--rm"
           "-u"
           (str user-id ":" group-id)
           "-v"
           (str (-> mermaid-filepath
                    build-filename/extract-path
                    build-filename/absolutize)
                ":/data")
           "-v"
           (str (build-filename/absolutize target-dir) ":/out-data")
           "minlag/mermaid-cli"
           "-i"
           mermaid-filename
           "-o"
           (build-filename/create-file-path "/out-data" image-filename)]}))

(defn need-to-update?
  "Returns true if `file-in` needed to be modified, compared to `file-out`."
  [file-in file-out]
  (and (build-filename/match-extension? file-in ".mermaid")
       (seq (build-file/modified-since file-out [file-in]))))

(defn build-mermaid-image-cmd
  "Returns a map with

  * `cmd` a command to turn `mermaid-filename` into an image with extension `image-extension`.
  * and `target-path` the name of the image generated."
  [mermaid-filename image-extension user-id group-id]
  (let [filename (build-filename/filename mermaid-filename)]
    {:command ["docker"
               "run"
               "--rm"
               "-u"
               (str user-id ":" group-id)
               "-v"
               (str (-> (build-filename/extract-path mermaid-filename)
                        build-filename/absolutize)
                    ":/data")
               "minlag/mermaid-cli"
               "-i"
               filename
               "-o"
               (build-filename/change-extension filename image-extension)]
     :target-path (build-filename/change-extension mermaid-filename image-extension)}))

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
                  (let [file-out (build-filename/change-extension file image-extension)]
                    (and (build-file/is-existing-file? file) (need-to-update? file file-out)))))))

(ns automaton-build.doc.mermaid-test
  (:require
   [automaton-build.doc.mermaid             :as sut]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.os.user                 :as build-user]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd]]
   [clojure.test                            :refer [deftest is]]))

(def filename (build-filename/create-file-path (build-file/create-temp-dir) "foo.mermaid"))
(deftest mermaid-build-image-cmd-test
  (let [filename (build-filename/create-file-path (build-file/create-temp-dir) "foo.mermaid")
        _ (build-file/write-file filename "erDiagram\ne ||--|| f: z")
        {:keys [output-file cmd]} (sut/mermaid-build-image-cmd filename
                                                               (-> (build-user/user-id-cmd)
                                                                   (blocking-cmd "" "" false)
                                                                   build-user/id-analyze)
                                                               (-> (build-user/group-id-cmd)
                                                                   (blocking-cmd "" "" false)
                                                                   build-user/id-analyze)
                                                               (build-file/create-temp-dir
                                                                "out-data"))]
    (blocking-cmd cmd "." nil false)
    (is (build-file/is-existing-file? output-file) "Is the file generated?")))

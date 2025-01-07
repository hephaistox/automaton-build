(ns automaton-build.tasks.impl.reports
  "Common reporting features."
  (:require
   [automaton-build.echo.headers :refer [h2-error! h2-valid! normalln uri-str]]
   [automaton-build.os.file      :as build-file]
   [automaton-build.os.filename  :as build-filename]))

(defn save-report!
  "Save on disk the content of the report. If it is empty, the file is deleted.

  Returns `true` if the report wasn't empty."
  [report-content filename]
  (build-file/ensure-dir-exists (build-filename/extract-path filename))
  (if (empty? report-content)
    (build-file/delete-file filename)
    (do (normalln "Reports saved in " (uri-str filename))
        (build-file/pp-file filename report-content)))
  (empty? report-content))

(defn synthesis
  "Prints a synthesis line for `status`."
  [status]
  (cond
    (nil? status) nil
    (true? status) (h2-valid! "Reports ok")
    (false? status) (h2-error! "Reports have failed")))

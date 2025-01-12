(ns automaton-build.headers.reports-fw
  "Scan code files for forbidden words.

  The forbidden words ca be setup in the `project.edn` under the `[:code :forbidden-words]`

  It will report whatever word is set this project.edn, except infiles which first line is:
  #\":heph-ignore\\s*\\{[^\\}]*:forbidden-words\".

  that will ignore `forbidden-words`."
  (:require
   [automaton-build.code.files           :as build-code-files]
   [automaton-build.code.forbidden-words :as build-code-fw]
   [automaton-build.echo.headers         :refer [h1 h1-error h1-valid h2-error! h2-valid! normalln]]
   [automaton-build.os.file              :as build-file]))

;; ********************************************************************************
;; Private
;; ********************************************************************************

(defn- project-forbidden-words
  [project-map]
  (-> project-map
      (get-in [:project-config-filedesc :edn :code :forbidden-words])))

(defn- generate-report-data
  "Returns the report data for the matches of `forbidden-words` in the file in `file-descs`."
  [filenames forbidden-words]
  (let [file-descs (map build-file/read-file filenames)
        regexp (build-code-fw/coll-to-alternate-in-regexp forbidden-words)]
    (->> file-descs
         (map (fn [{:keys [raw-content filename]
                    :as _file-desc}]
                {:filename filename
                 :res (build-code-fw/forbidden-words-matches regexp raw-content)}))
         (filterv (comp not empty? :res)))))

;; ********************************************************************************
;; API
;; ********************************************************************************

(defn project-report
  [project-map]
  (let [forbidden-words (project-forbidden-words project-map)
        project-filenames (->> project-map
                               :deps
                               ((juxt :dir :edn))
                               (apply build-code-files/project-dirs)
                               build-code-files/project-files)]
    (generate-report-data project-filenames forbidden-words)))

(defn report
  "Reports all forbidden words found in the files in the `file-descs`, they're in the report if the file contains `forbidden-words`.

  Returns `true` if ok."
  [{:keys [subprojects]
    :as project-map}]
  (h1 "Scan for forbidden words")
  (let [res (if-let [subprojects subprojects]
              (->> subprojects
                   (mapcat project-report))
              (project-report project-map))]
    (normalln "Errors")
    (run! normalln res)
    (if (empty? res)
      (h1-valid "No forbidden words found.")
      (h1-error "Some forbidden words have been found."))
    (empty? res)))

(defn synthesis
  "Prints a synthesis line for `status`."
  [status]
  (cond
    (nil? status) nil
    (true? status) (h2-valid! "No forbidden words found.")
    (false? status) (h2-error! "Some forbidden words have been found.")))


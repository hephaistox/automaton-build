(ns automaton-build.tasks.impl.reports-fw
  "Scan code files for forbidden words.

  The forbidden words ca be setup in the `project.edn` under the `[:code :forbidden-words]`

  It will report whatever word is set this project.edn, except infiles which first line is:
  #\":heph-ignore\\s*\\{[^\\}]*:forbidden-words\".

  that will ignore `forbidden-words`."
  (:require
   [automaton-build.code.files           :as build-code-files]
   [automaton-build.code.forbidden-words :as build-code-fw]
   [automaton-build.echo.headers         :refer [h1
                                                 h1-error
                                                 h1-valid
                                                 h2-error!
                                                 h2-valid!]]
   [automaton-build.os.file              :as build-file]
   [automaton-build.tasks.impl.reports   :as build-tasks-reports]))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(defn- project-files
  "Returns project file descriptions found in the project in `project-dir`."
  [deps-edn-filedesc]
  (->> deps-edn-filedesc
       build-code-files/project-dirs
       build-code-files/project-files
       (map build-file/read-file)))

(defn- generate-report-data
  "Returns the report data for the matches of `forbidden-words` in the file in `file-descs`."
  [file-descs forbidden-words]
  (let [regexp (build-code-fw/coll-to-alternate-in-regexp forbidden-words)]
    (->> file-descs
         (map (fn [{:keys [raw-content filename]
                    :as _file-desc}]
                {:filename filename
                 :res (build-code-fw/forbidden-words-matches regexp
                                                             raw-content)}))
         (filterv (comp not empty? :res)))))

(def report-file-path "docs/reports/fw.edn")

;; ********************************************************************************
;; API
;; ********************************************************************************
(defn report
  "Reports all forbidden words found in the files in the `file-descs`, they're in the report if the file contains `forbidden-words`.

  Returns `true` if ok."
  [file-descs forbidden-words]
  (h1 "Scan for forbidden words")
  (let [res (generate-report-data file-descs forbidden-words)]
    (build-tasks-reports/save-report! res report-file-path)
    (if (empty? res)
      (h1-valid "No forbidden words found.")
      (h1-error "Some forbidden words have been found."))
    (empty? res)))

(defn report-monorepo
  [{:keys [subprojects]
    :as _monorepo-project-map}]
  (h1 "Scan for forbidden words")
  (let [res
        (->> subprojects
             (mapcat
              (fn [subproject]
                (let [forbidden-words
                      (get-in
                       subproject
                       [:project-config-filedesc :edn :code :forbidden-words])
                      project-file-descs (->> subproject
                                              :deps
                                              project-files)]
                  (generate-report-data project-file-descs forbidden-words)))))]
    (if (empty? res)
      (h1-valid "No forbidden words found.")
      (h1-error "Some forbidden words have been found."))
    (build-tasks-reports/save-report! res report-file-path)
    (empty? res)))

(defn synthesis
  "Prints a synthesis line for `status`."
  [status]
  (cond
    (nil? status) nil
    (true? status) (h2-valid! "No forbidden words found.")
    (false? status) (h2-error! "Some forbidden words have been found.")))


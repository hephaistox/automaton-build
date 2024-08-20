(ns automaton-build.tasks.impl.report-aliases
  "Report aliases inconsistency:
  * One `ns` with more than one alias,
  * One `alias` assigned to more than one namespace.."
  (:require
   [automaton-build.code.files         :as build-code-files]
   [automaton-build.code.reports       :as build-code-reports]
   [automaton-build.echo.headers       :refer [build-writter
                                               h1
                                               h1-error
                                               h1-valid
                                               h2-error!
                                               h2-valid!
                                               normalln
                                               print-writter]]
   [automaton-build.os.file            :as build-file]
   [automaton-build.tasks.impl.reports :as build-tasks-reports]
   [clojure.set                        :as set]))

(defn project-files
  "Returns project file descriptions found in the project in `project-dir`."
  [deps-edn-filedesc]
  (->> deps-edn-filedesc
       build-code-files/project-dirs
       build-code-files/project-files
       (map build-file/read-file)))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(defn alias-list
  "From a list of project file description `project-file-descs` (with file description, content and status).

  Note that the scope of this `project-file-descs` is important. If you want to check consistancy between two projects, their files should be included in this list.

  Return `matches`, a list of map with keys `:filename`, `:ns` and `:alias` for all matches found in the content."
  [project-file-descs]
  (->> project-file-descs
       (mapcat (fn [project-file-desc]
                 (->> (build-code-reports/search-aliases project-file-desc)
                      (remove (fn [{:keys [alias]}]
                                (contains? #{"sut" nil} alias))))))
       vec))

;; ********************************************************************************
;; Alias actions definitions
;; ********************************************************************************
(defn scan-alias-project*
  "For all `project-file-descs` in a project, their alias consistency is analyzed, reports are saved if exist.

  Has side effects and echo the result.

  Returns `true` if all aliases are consistent."
  [project-file-descs verbose]
  (h1 "Search for alias inconsistencies.")
  (let [cleaned-project-file-descs
        (remove (comp build-code-reports/is-ignored-file? :raw-content)
                project-file-descs)
        s (build-writter)
        res (binding [*out* s]
              (let [matches (alias-list cleaned-project-file-descs)
                    clj-files-wo-aliases
                    (set/difference (set (mapv :filename
                                               cleaned-project-file-descs))
                                    (set (mapv :filename matches)))]
                (when verbose
                  (normalln "found"
                            (count project-file-descs)
                            "files,"
                            (- (count project-file-descs)
                               (count cleaned-project-file-descs))
                            "ignored and found"
                            (count matches)
                            "matches, "
                            (count clj-files-wo-aliases)
                            "files with no alias."))
                (and (-> (build-code-reports/alias-inconsistent-ns matches)
                         (build-tasks-reports/save-report!
                          "docs/reports/ns-inconsistent-alias.edn"))
                     (-> (build-code-reports/ns-inconsistent-aliases matches)
                         (build-tasks-reports/save-report!
                          "docs/reports/alias-inconsistent-ns.edn")))))]
    (if res
      (h1-valid "Alias are consistent.")
      (h1-error "Alias inconsistency found."))
    (print-writter s)
    res))

(defn scan-alias
  "Scan all subproject of `monorepo-project-map` to generate their alias report.

  Note all inconsistencies are searched across projects also.
  Return `true` if all projects are valid."
  [monorepo-project-map verbose]
  (let [project-files-descs (->> (:subprojects monorepo-project-map)
                                 (mapcat (fn [subproject]
                                           (->> subproject
                                                :deps
                                                project-files)))
                                 vec)]
    (scan-alias-project* project-files-descs verbose)))

(defn synthesis
  "Prints a synthesis line for `status`."
  [status]
  (cond
    (nil? status) nil
    (true? status) (h2-valid! "Alias are ok")
    (false? status) (h2-error! "Alias are unconsistent")))

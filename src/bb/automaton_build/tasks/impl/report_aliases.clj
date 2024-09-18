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
                                               print-writter
                                               uri-str]]
   [automaton-build.os.file            :as build-file]
   [automaton-build.tasks.impl.reports :as build-tasks-reports]
   [clojure.set                        :as set]
   [clojure.string                     :as str]))

(defn project-files
  "Returns project file descriptions found in the project in `project-dir`."
  [deps-edn-filedesc]
  (let [deps-edn-dir (:dir deps-edn-filedesc)
        deps (:edn deps-edn-filedesc)]
    (->> (build-code-files/project-dirs deps-edn-dir deps)
         build-code-files/project-files
         (map build-file/read-file))))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(defn alias-list
  "From a list of project file description `project-file-descs` (with file description, content and status).

  Note that the scope of this `project-file-descs` is important. If you want to check consistancy between two projects, their files should be included in this list.

  Return `matches`, a list of map with keys `:filename`, `:ns` and `:alias` for all matches found in the content."
  [project-file-descs]
  (->> project-file-descs
       (mapcat (fn [{:keys [raw-content]
                     :as project-file-desc}]
                 (->> (build-code-reports/search-aliases project-file-desc)
                      (map (fn [{:keys [alias]
                                 :as alias-map}]
                             (cond-> alias-map
                               (= "sut" alias) (assoc :sut-alias true :skip true)
                               (nil? alias) (assoc :nil-alias true :skip true)
                               (build-code-reports/is-ignored-file? raw-content)
                               (assoc :ignored-file true :skip true)))))))
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
  (let [s (build-writter)
        res
        (binding [*out* s]
          (let [matches (alias-list project-file-descs)
                non-skipped-matches (remove :skip matches)
                clj-files-wo-aliases (set/difference (set (mapv :filename project-file-descs))
                                                     (set (mapv :filename non-skipped-matches)))]
            (when verbose
              (normalln "found"
                        (count project-file-descs)
                        "files,"
                        (count matches)
                        "matches, with "
                        (count (filter :skip matches))
                        "ignored (sut:"
                        (count (filter :sut-alias matches))
                        ", nil:"
                        (count (filter :nil-alias matches))
                        ", ignored:"
                        (count (filter :ignored-file matches))
                        ") and "
                        (count clj-files-wo-aliases)
                        "files with no alias.")
              (normalln "ignored files are:"
                        (vec (distinct (map :filename (filter :ignored-file matches)))))
              (let [filename (build-file/create-temp-file "matches.edn")]
                (build-file/write-file filename (str/replace (pr-str matches) #"}" "}\n"))
                (normalln "Find details of matches here:" (uri-str filename))))
            (and (-> (build-code-reports/alias-inconsistent-ns clj-files-wo-aliases)
                     (build-tasks-reports/save-report! "docs/reports/ns-inconsistent-alias.edn"))
                 (-> (build-code-reports/ns-inconsistent-aliases clj-files-wo-aliases)
                     (build-tasks-reports/save-report!
                      "docs/reports/alias-inconsistent-ns.edn")))))]
    (if res (h1-valid "Alias are consistent.") (h1-error "Alias inconsistency found."))
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

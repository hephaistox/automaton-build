(ns automaton-build.code.report-aliases
  "Report aliases inconsistency:
  * One `ns` with more than one alias,
  * One `alias` assigned to more than one namespace.."
  (:require
   [automaton-build.code.files   :as build-code-files]
   [automaton-build.code.reports :as build-code-reports]
   [automaton-build.os.file      :as build-file]
   [clojure.set                  :as set]
   [clojure.string               :as str]))

;; ********************************************************************************
;; Project directories
;; ********************************************************************************
(defn project-dirs
  "Return all directories of the `deps.edn` file as in `deps-edn-filedesc`"
  [deps-edn-filedesc]
  (build-code-files/project-dirs (:dir deps-edn-filedesc) (:edn deps-edn-filedesc)))

(defn project-files
  "Returns a vector of project file descriptions as found in the project in `project-dir`."
  [project-dirs]
  (->> project-dirs
       build-code-files/project-files
       (mapv build-file/read-file)))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(defn alias-list
  "List aliases in the files from `project-file-descs`.

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
  [{:keys [normalln uri-str h1 h2 h1-valid! h1-error!]
    :as _printers}
   project-file-descs
   verbose?]
  (h1 "Search for alias inconsistencies.")
  (let [matches (alias-list project-file-descs)
        non-skipped-matches (remove :skip matches)
        clj-files-wo-aliases (set/difference (set (mapv :filename project-file-descs))
                                             (set (mapv :filename non-skipped-matches)))]
    (when verbose?
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
    (let [alias-inconsistents (build-code-reports/alias-inconsistent-ns clj-files-wo-aliases)
          ns-inconsistentes (build-code-reports/ns-inconsistent-aliases clj-files-wo-aliases)
          alias-valid? (and (empty? alias-inconsistents) (empty? ns-inconsistentes))]
      (if alias-valid?
        (h1-valid! "Alias are consistent.")
        (do (h2 "That aliases are not consistent across namespaces")
            (normalln (pr-str alias-inconsistents))
            (h2 "That namespaces are not consistent aliases")
            (normalln (pr-str alias-inconsistents))
            (h1-error! "Alias inconsistency found.")))
      alias-valid?)))

(defn scan-alias
  "Scan all subproject of `monorepo-project-map` to generate their alias report.

  Note all inconsistencies are searched across projects also.
  Return `true` if all projects are valid."
  [{:keys [subprojects]
    :as _monorepo-project-map}
   verbose?]
  (-> (mapcat (fn [{:keys [deps]
                    :as _subproject}]
                (project-files deps))
              subprojects)
      (scan-alias-project* verbose?)))

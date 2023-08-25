(ns automaton-build.adapters.code-files
  "Proxy for clojure code files"
  (:require
   [clojure.string :as str]

   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-build.adapters.schema :as schema]
   [automaton-core.adapters.edn-utils :as edn-utils]))

(def files-repo-schema
  "Schema for files-repo"
  [:map-of :string [:vector :string]])

(def code-extensions
  "**{.clj,.cljs,.cljc,.edn}")

(defn validate
  "Validate the file repository
  Params:
  * `files-repo` validate the files repository"
  [files-repo]
  (schema/schema-valid files-repo-schema
                       files-repo))

(defn code-files-name
  "Return the list of the file names
  Params:
  * `dir` the directory where files are searched"
  [dir]
  (files/search-files dir
                      code-extensions))

(defn code-files-repo
  "Return a map of filename associated with their content, e.g.
  `{\"core.clj\" \"core.clj file content\"}`
  Params:
  * `dir` the directory where files are searched"
  [dir]
  (into {}
        (->>
         (files/create-files-map dir
                                 code-extensions)
         (map (fn [[filename content]]
                [filename (str/split-lines content)])))))

(defn get-clj*-files
  "Filter clojure code files in the `files-repo`
  Params:
  * `files-repo` repository to filter"
  [files-repo]
  (into {}
        (filter (fn [[filename _]]
                  (edn-utils/is-clojure-like-file filename))
                files-repo)))

(defn exclude-files
  "Exclude in the `files-repo` the files in the `excluded-files` set of filenames
  Params:
  * `files-repo` repository to filter
  * `excluded-files` sequence of file names that will not be included in the filename"
  [files-repo excluded-files]
  (let [excluded-files (into #{} excluded-files)]
    (into {}
          (filter (fn [[filename]]
                    (not (contains? excluded-files
                                    (files/file-name filename))))
                  files-repo))))

(defn search-line
  "Search the pattern in a line
  Params:
  * `pattern` pattern to search
  * `file-line` data to search in"
  [pattern file-line]
  (re-find pattern file-line))

(defn create-report
  "The report is a list of matches of `pattern` in each file of `file-repo`
  Params:
  * `files-repo` file repository
  * `pattern` pattern to search in all files"
  [files-repo pattern]
  (into []
        (mapcat (fn [[filename file-content]]
                  (filter (comp not empty? second)
                          (map (fn [file-line]
                                 [filename (search-line pattern file-line)])
                               file-content)))
                files-repo)))

(defn save-report
  "Save the report so that users can see the detailed results
  Title is a string with one `%s` parameter which will be replaced with the content of the report
  Params:
  * `report` a report created by `create-report`
  * `report-title` a human readable title of that report"
  [report report-title]
  (log/info report-title)
  (when-not (empty? report)
    (log/trace "Results found:" (edn-utils/spit-in-tmp-file report)))
  report)

(defn filter-report
  "Filter the `report` with the `filter-fn`
  Params:
  * `report` a report created with `create-report`
  * `filter-fn` a function that returns true if we keep the value, function has two parameters `filename` the name of the file and `matches` is the list of matches"
  [report filter-fn]
  (filter (fn [[filename matches]]
            (filter-fn filename
                       matches))
          report))

(defn map-report
  "Map the report to change each report line
  Params:
  * `report` a report created with `create-report`
  * `update-fn` to update a line, with filename and matches as parameters"
  [report update-fn]
  (map (fn [[filename matches]]
         (update-fn filename matches))
       report))

(defn group-by-report
  "Creates groups on the report,
  Params:
  * `report` a report created with `create-report`
  * The `group-by-fn` is used to create the aggregates with (group-by-fn [report-line])
  * The `aggregation` is used to create a vector, each line in the report for that group is called with (aggregation report-line) to tell what to keep for that record in the aggregates"
  [report group-by-fn aggregation empty-result]
  (->> (group-by group-by-fn
                 report)
       (map (fn [[group data]]
              (conj group
                    (reduce (fn [aggregated item]
                              (conj aggregated
                                    (aggregation item)))
                            empty-result
                            data))))))

(defn print-report
  "Apply the `printer` to each line of the `report`,
  Params:
  * `report` a report created with `create-report`
  * `printer` a function to print each line"
  [report printer]
  (doseq [report-line report]
    (printer report-line))
  report)

(defn assert-empty
  "Assert if the report is non empty
   Params:
  * `report` a report created with `create-report`
  * `title` the title to display if the report is not empty"
  [report title]
  (when-not (empty?  report)
    (throw (ex-info title
                    {:report report})))
  report)

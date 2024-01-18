(ns automaton-build.file-repo.text-matches
  "Find matches in a text repo, it consists of lines of analyze

  We call analysis the process and analyze the result of it"
  (:require
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.log :as build-log]
   [clojure.string :as str]))

(defn filter-matches
  "Filter with the `filter-fn`
  Params:
  * `matches`
  * `filter-fn` a function that returns true if we keep the value, function has two parameters `filename` the name of the file and `matches` is the list of matches"
  [matches filter-fn]
  (->> matches
       (filter (fn [[filename matches]] (filter-fn filename matches)))))

(defn save-as-file
  "Save the matches so that users can see the detailed results
  Params:
  * `matches`
  * `report-title` a human readable title of that report. Contains one `%s` parameter which will be replaced with the content of the report
  * `report-filename` where to store the report"
  [matches report-title report-filename]
  (build-log/info-format "Save report `%s`" report-title)
  (when (and (not (str/blank? report-filename)) (seq matches))
    (build-edn-utils/spit-edn report-filename matches)))

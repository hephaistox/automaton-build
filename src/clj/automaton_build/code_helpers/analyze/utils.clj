(ns automaton-build.code-helpers.analyze.utils
  "Helpers function to manage analyze of code"
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.edn-utils  :as build-edn-utils]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files      :as build-files]))

(defn- matches-to-output-lines [match-to-output-line matches] (mapv match-to-output-line matches))

(defn save-report
  "Matches
  Params:
  * `matches` output of a previous run analysis, content is depending on each analysis
  * `report-title` string that will be added in the header of the file
  * `filename` string of the path where to save
  * `match-to-output-line` function for each line, passing this line as an argument and returning what is expected to be outputted in the file"
  [matches report-title filename match-to-output-line]
  (if (empty? matches)
    (build-files/delete-files [filename])
    (do (build-log/info report-title)
        (build-edn-utils/spit-edn filename
                                  (->> matches
                                       (matches-to-output-lines match-to-output-line))
                                  report-title)
        matches)))

(defn assert-empty
  [matches filename assert-message]
  (when-not (empty? matches)
    (build-log/warn-data matches (format "%s - open file `%s` for details" assert-message filename))
    true))

(defn when-broken-exit
  [rule-broken?]
  (when rule-broken? (System/exit build-exit-codes/rules-broken)))

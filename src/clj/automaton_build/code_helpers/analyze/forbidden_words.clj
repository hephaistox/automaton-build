(ns automaton-build.code-helpers.analyze.forbidden-words
  "Search for keywords in the project"
  (:require
   [automaton-build.code-helpers.analyze.utils :as build-analyze-utils]
   [automaton-build.file-repo.text :as build-filerepo-text]
   [automaton-build.log :as build-log]
   [clojure.string :as str]))

(defn coll-to-alternate-in-regexp
  "Turn a collection of strings (or patterns) like (\"a\" \"b\") to \"(a|b)\"
  Params:
  * `coll` collection to transform. Each element could be a string or a regexp"
  [coll]
  (if (empty? coll)
    nil
    (->> coll
         (map str)
         (str/join "|")
         (format "(%s)")
         re-pattern)))

(defn forbidden-words-matches
  "Creates the list of namespaces, their alias and filenames, save it in a report
  All namespaces with an alias are listed, except the sut alias as it is a special case

  Returns the matches

  Params:
  * `regexp` regexp (with groups) of strings to search
  * `clj-repo`"
  [regexp clj-repo]
  (build-log/info "Forbidden words analysis")
  (if-not regexp
    (build-log/warn "regexp is empty, analysis aborted")
    (let [matches (-> clj-repo
                      (build-filerepo-text/filecontent-to-match
                       regexp
                       [:forbidden-words]))]
      (->> matches
           (mapv (fn [[filename [whole-match & matches]]] [filename
                                                           (vec matches)
                                                           whole-match]))))))

(defn save-report
  [matches filename]
  (build-analyze-utils/save-report
   matches
   "List of forbidden words found in the project"
   filename
   str))

(defn assert-empty
  [matches filename]
  (build-analyze-utils/assert-empty
   matches
   filename
   "That words should not appear in the project"))

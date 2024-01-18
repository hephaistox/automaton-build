(ns automaton-build.code-helpers.analyze.namespace-has-one-alias
  "Search for namespaces which are aliazed many times"
  (:require
   [automaton-build.code-helpers.analyze.utils :as build-analyze-utils]
   [automaton-build.file-repo.text :as build-filerepo-text]
   [automaton-build.log :as build-log]))

(def alias-pattern
  #"^\s*(?:\[\s*|\(:require\s*\[\s*)([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)\s*(?:(?::as)\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)|(:refer).*)\s*\]\s*(?:\)\))*$")

(defn- search-alias-with-multiple-namespaces
  [matches]
  (->> matches
       (group-by (fn [[_filename namespace alias :as _match]] [namespace
                                                               alias]))
       (mapv (fn [[k-alias-ns match]] [k-alias-ns (mapv first match)]))
       (group-by ffirst)
       (filter (fn [[_k-alias-ns files]] (> (count files) 1)))
       vals
       (apply concat)
       vec
       (group-by ffirst)))

(defn alias-matches
  "Creates the list of namespaces, their alias and filenames, save it in a report
  All namespaces with an alias are listed, except the sut alias as it is a special case

  Params:
  * `clj-repo`"
  [clj-repo]
  (build-log/info "Aliases analysis")
  (let [matches (-> clj-repo
                    (build-filerepo-text/filecontent-to-match alias-pattern))]
    (->> matches
         (map (fn [[filename [whole-match namespace alias _refer?]]]
                [filename namespace alias whole-match]))
         (filter (fn [[_filename namespace alias]]
                   (not (or (= "sut" alias)
                            (nil? alias)
                            (= "clojure.deftest" namespace)))))
         search-alias-with-multiple-namespaces
         (into {}))))

(defn save-report
  [matches filename]
  (build-analyze-utils/save-report
   matches
   "List of namespaces referenced by many aliases"
   filename
   identity))

(defn assert-empty
  [matches filename]
  (build-analyze-utils/assert-empty
   matches
   filename
   "Found namespace represented with more than one alias"))

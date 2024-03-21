(ns automaton-build.file-repo.text
  "Repository of files associating the name of a file to its content
  This repo deal with text files and will split the file in vector of lines"
  (:require
   [automaton-build.file-repo.raw      :as build-filerepo-raw]
   [automaton-build.file-repo.raw.impl :as build-raw-impl]
   [automaton-build.os.files           :as build-files]
   [automaton-build.utils.string       :as build-string]
   [clojure.string                     :as str]))

(defrecord TextFileRepo [_file-repo-map]
  build-filerepo-raw/FileRepo
    (exclude-files [_ excluded-files]
      (-> _file-repo-map
          (build-raw-impl/exclude-files excluded-files)
          ->TextFileRepo))
    (file-repo-map [_] _file-repo-map)
    (filter-repo [_ filter-fn]
      (->> (build-raw-impl/filter-repo-map _file-repo-map filter-fn)
           ->TextFileRepo))
    (nb-files [_] (count _file-repo-map))
    (filter-by-extension [_ extensions]
      (-> _file-repo-map
          (build-raw-impl/filter-by-extension extensions)
          ->TextFileRepo)))

(defn make-text-file-map
  [filenames]
  (->> (build-raw-impl/repo-map filenames)
       (map (fn [[filename content]] [filename (str/split-lines content)]))
       (into {})))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn make-text-file-repo
  "Creates an instance of TextFileRepository"
  [filenames]
  (-> (make-text-file-map filenames)
      ->TextFileRepo))

(defn search-line
  "Search the pattern in a line
  Params:
  * `pattern` pattern to search
  * `file-line` data to search in"
  [pattern file-line]
  (when pattern
    (let [pattern (re-pattern pattern)] (re-find pattern file-line))))

(defn ignore-rule-config
  [filecontent]
  (build-string/str->map (re-find #"\{:heph-ignore.*"
                                  (str/join (take 10 filecontent)))))

(defn ignore-match?
  [line-match ignore-values]
  (some (fn [ignore-val]
          (some (fn [match-val] (= match-val ignore-val)) line-match))
        ignore-values))

(defn ignore-all? [rules] (some #(true? %) rules))

(defn ignore-config-rule-applies?
  [rules line-match]
  (if (ignore-all? rules) true (ignore-match? line-match rules)))

(defn match-not-excluded?
  [ignored-matches file-content line-match]
  (let [ignore-config (ignore-rule-config file-content)
        config-matching-rules (select-keys (:heph-ignore ignore-config)
                                           ignored-matches)]
    (or (empty? ignored-matches)
        (nil? ignore-config)
        (empty? config-matching-rules)
        (not (ignore-config-rule-applies? (flatten (vals config-matching-rules))
                                          line-match)))))

(defn filecontent-to-match
  "Apply the pattern to each line of each file of the repo
  The result is a vector containing as many elements as there are matches.
  Each element contains the filename where the match has been found, and the match itself
  Params:
  * `text-file-repo` an instance of `TextFilesRepository`
  * `pattern` pattern to search, is a regexp, strings are transformed to regexp"
  ([text-file-repo pattern] (filecontent-to-match text-file-repo pattern []))
  ([text-file-repo pattern ignored-matches]
   (->> text-file-repo
        build-filerepo-raw/file-repo-map
        (mapcat (fn [[filename file-content]]
                  (->> file-content
                       (map (fn [file-line]
                              [(build-files/relativize-to-pwd filename)
                               (when-let [line-match (search-line pattern
                                                                  file-line)]
                                 (when (match-not-excluded? ignored-matches
                                                            file-content
                                                            line-match)
                                   line-match))]))
                       (filter (comp not empty? second)))))
        vec)))

(ns automaton-build.file-repo.raw.impl
  "Implementations an helper functions for raw file repo"
  (:require
   [automaton-build.log      :as build-log]
   [automaton-build.os.files :as build-files]
   [clojure.string           :as str]))

(defn filter-repo-map
  "Filter a file repo map with the filter-fn
  Returns the filtered map
  Params
  * `file-repo-map` a map of filename and file content
  * `filter-fn` is a function applied to each element of the repo map"
  [file-repo-map filter-fn]
  (if filter-fn
    (->> (filter filter-fn file-repo-map)
         (into {}))
    file-repo-map))

(defn filter-by-extension
  "Filter a file repo map with the extensions provided as a parameter
  Params:
  * `file-repo-map` a map of filename and file content
  * `extensions` list of strings that will be used to filter the map"
  [file-repo-map extensions]
  (filter-repo-map file-repo-map
                   (fn [[filename _]]
                     (apply build-files/match-extension? filename extensions))))

(defn exclude-files
  "Exclude
  Params:
  * `file-repo-map` map for file repo
  * `excluded-files` "
  [file-repo-map excluded-files]
  (let [excluded-files (into #{} excluded-files)]
    (->> file-repo-map
         (filter (fn [[filename]]
                   (let [res (some (fn [excluded-file]
                                     (str/includes? filename excluded-file))
                                   excluded-files)]
                     (when res
                       (build-log/trace-format "File `%s` is removed from repo"
                                               filename))
                     (not res))))
         (into {}))))

(defn repo-map
  "Create the repo map of files, associate absolutized filename to its content
  Params:
  * `filenames` is the collection of files to return, non existing files are skipped"
  [filenames]
  (->> (apply build-files/filter-to-existing-files filenames)
       (map (fn [filename]
              (let [abs-filename (build-files/absolutize filename)]
                [(str abs-filename) (build-files/read-file abs-filename)])))
       (into {})))

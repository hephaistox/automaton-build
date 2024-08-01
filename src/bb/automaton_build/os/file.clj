(ns automaton-build.os.file
  "Tools to manipulate local files

  Is a proxy to babashka.fs tools."
  (:require
   [automaton-build.os.filename :as build-filename]
   [babashka.fs                 :as fs]
   [clojure.pprint              :as pp]
   [clojure.string              :as str]))

(defn expand-home-str
  "In string `str`, ~ is expanded to the actual value of home directory."
  [str]
  (-> str
      fs/path
      fs/expand-home
      clojure.core/str))

(defn is-existing-path?
  "Returns true if `filename` path already exist."
  [path]
  (when-not (str/blank? path) (when (fs/exists? path) path)))

(defn is-existing-file?
  "Returns true if `filename` path already exist and is not a directory."
  [filename]
  (when (and (is-existing-path? filename) (not (fs/directory? filename)))
    filename))

(defn is-existing-dir?
  "Check if this the path exist and is a directory."
  [dirname]
  (when (and (is-existing-path? dirname) (fs/directory? dirname)) dirname))

(defn create-file-path
  "Creates a path for which each element of `dirs` is a subdirectory."
  [& dirs]
  (-> (if (some? dirs)
          (->> dirs
               (mapv str)
               (filter #(not (str/blank? %)))
               (mapv build-filename/remove-trailing-separator)
               (interpose build-filename/directory-separator)
               (apply str))
          "./")
      str))

(defn write-file
  "Write the text file `filename` with its `content`."
  [filename content]
  (spit filename content))

(defn read-file
  "Read the file named `target-filename`.
  Please test file existence if you need more informations."
  [target-filename]
  (slurp (expand-home-str target-filename)))

(defn modified-since
  "Returns `true` if `anchor` is older than one of the file in `file-set`."
  [anchor file-set]
  (let [file-set (filter some? file-set)]
    (when anchor (seq (fs/modified-since anchor file-set)))))

(defn matching-files
  "Match files recursively found in `dir` that are matching `file-pattern`."
  [dir file-pattern]
  (fs/glob dir file-pattern))

(defn combine-files
  "Read text content of files `filenames` and combine them into `target-filename`."
  [target-filename & filenames]
  (->> (map read-file filenames)
       (apply str)
       (write-file target-filename)))

(defn create-temp-file
  "Create a temporary file with `filename` name of the file (optional)."
  [& filename]
  (-> (fs/create-temp-file {:suffix (apply str filename)})
      str))

(defn pp-file
  "Pretty print the file."
  [filename file-content]
  (->> (with-out-str (pp/pprint file-content))
       (spit filename)))

(defn delete-file
  "Deletes `filename` if exists."
  [filename]
  (when (is-existing-file? filename) (fs/delete filename)))

(ns automaton-build.os.file
  "Tools to manipulate local files

  Is a proxy to babashka.fs tools."
  (:require
   [automaton-build.os.filename :as build-filename]
   [babashka.fs                 :as fs]
   [clojure.pprint              :as pp]
   [clojure.string              :as str]))

;; ********************************************************************************
;; Filenames based on local file structure

(defn expand-home-str
  "In string `str`, ~ is expanded to the actual value of home directory."
  [str]
  (-> str
      fs/path
      fs/expand-home
      clojure.core/str))

;; ********************************************************************************
;; Directory manipulation

(defn is-existing-dir?
  "Check if this the path exist and is a directory."
  [dirname]
  (when (and (not (str/blank? dirname)) (fs/exists? dirname) (fs/directory? dirname)) dirname))

(defn delete-dir
  "Deletes `dir` and returns it.
   If `dir` does not exist, returns nil"
  [dir]
  (when (is-existing-dir? dir) (fs/delete-tree dir) dir))

(defn ensure-dir-exists
  "Creates directory `dir` if not already existing."
  [path]
  (when (string? path) (when-not (is-existing-dir? path) (fs/create-dirs path))))

(defn empty-dir "Empty the directory `path`." [path] (delete-dir path) (ensure-dir-exists path))

(defn copy-dir [src-path dst-path options] (fs/copy-tree src-path dst-path options))

;; ********************************************************************************
;; File manipulation

(defn is-existing-file?
  "Returns true if `filename` path already exist and is not a directory."
  [filename]
  (when (and (not (str/blank? filename)) (fs/exists? filename) (fs/regular-file? filename))
    filename))

(defn delete-file
  "Deletes `filename` and returns it.
   If `filename` does not exist, returns nil."
  [filename]
  (when (is-existing-file? filename) (fs/delete filename) filename))

(defn copy-file [src-path dst-path options] (fs/copy src-path dst-path options))

(defn make-executable
  "Make file `filename` executable."
  [filename]
  (when (is-existing-file? filename)
    (fs/set-posix-file-permissions filename (fs/str->posix "rwx------"))))

(defn create-sym-link
  "Creates a sym link to `target` linking to `path`."
  [path target]
  (fs/create-sym-link path target))

;; ********************************************************************************
;; Path manipulation

(defn is-existing-path?
  "Returns true if `filename` path already exist."
  [path]
  (when-not (str/blank? path) (when (fs/exists? path) path)))

(defn modified-since
  "Returns `true` if `anchor` is older than one of the file in `file-set`."
  [anchor file-set]
  (let [file-set (filter some? file-set)] (when anchor (seq (fs/modified-since anchor file-set)))))

(defn delete-path
  "Deletes `path` and returns it.
   Returns nil if the `path` does not exists."
  [path]
  (if (fs/directory? path) (delete-dir path) (delete-file path)))

(defn path-on-disk
  "Returns a map with informations on the existance of the file on disk
  It returns
  * `:path` given path
  * `:apath` the absolute path
  * `:directory?`
  * `:exist?`"
  [filepath]
  (cond-> {:path filepath
           :apath (build-filename/absolutize filepath)}
    (fs/regular-file? filepath) (assoc :file? true)
    (fs/directory? filepath) (assoc :directory? true)
    (fs/exists? filepath) (assoc :exist? true)))

;; ********************************************************************************
;; Temporaries

(defn create-temp-file
  "Create a temporary file with `filename` name of the file (optional)."
  [& filename]
  (-> (fs/create-temp-file {:suffix (apply str filename)})
      str))

(defn create-temp-dir
  "Returns a subdirectory path string of the system temporary directory.
  `sub-dirs` is an optional list of strings, each one is a sub directory."
  [& sub-dirs]
  (let [tmp-dir (apply build-filename/create-dir-path
                       (-> (fs/create-temp-dir)
                           str)
                       (interpose build-filename/directory-separator sub-dirs))]
    (ensure-dir-exists tmp-dir)
    tmp-dir))

;; ********************************************************************************
;; Modify file content

(defn write-file
  "Write the `content` in the file at `filepath` .

  `:status` is `:ok` or `:fail`
  When `status` is `:fail`, `:exception` contains why"
  [filepath content]
  (merge {:filename filepath
          :afilename (build-filename/absolutize filepath)
          :content content}
         (try (spit filepath content)
              {:status :ok}
              (catch Exception e
                {:exception e
                 :status :fail}))))

(defn read-file
  "Read the file named `filename`.

   Returns:

  * `dir` directory of filename
  * `filename`
  * `raw-content` if file can be read.
  * `invalid?` to `true` whatever why.
  * `exception` if something wrong happened."
  [filename]
  (let [filename (str filename)]
    (merge {:filename filename
            :afilename (build-filename/absolutize filename)
            :dir (build-filename/extract-path filename)}
           (try {:raw-content (slurp filename)}
                (catch Exception e
                  {:exception e
                   :invalid? true})))))

(defn combine-files
  "Read text content of files `filenames` and combine them into `target-filename`."
  [target-filename & filenames]
  (->> (map (comp :raw-content read-file) filenames)
       (apply str)
       (write-file target-filename)))

(defn pp-file
  "Pretty print the file."
  [filename file-content]
  (->> (with-out-str (pp/pprint file-content))
       (spit filename)))

;; ********************************************************************************
;; Search

(defn search-files
  "Search files and dirs.
  * `root-dir` is where the root directory of the search-files
  * `pattern` is a regular expression or a glob as described in [java doc](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
  * `options` (Optional, default = {}) are boolean value for `:hidden`, `:recursive` and `:follow-lins`. See [babashka fs](https://github.com/babashka/fs/blob/master/API.md#glob) for details.
  For instance:
  * `(files/search-files \"\" \"**{.clj,.cljs,.cljc,.edn}\")` search all clj file-paths in pwd directory"
  ([root-dir pattern options]
   (when (is-existing-dir? root-dir)
     (mapv str
           (fs/glob root-dir
                    pattern
                    (merge {:hidden true
                            :recursive true
                            :follow-links true}
                           options)))))
  ([root-dir pattern] (search-files root-dir pattern {})))

(defn matching-files
  "Match files recursively found in `dir` that are matching `file-pattern`."
  [dir file-pattern]
  (fs/glob dir file-pattern))

(defn search-in-parents
  "Search `file-or-dir` in the parents directories of `dir`."
  [dir file-or-dir]
  (loop [dir (build-filename/absolutize dir)]
    (let [file-candidate (build-filename/create-file-path (str dir) file-or-dir)]
      (if (fs/exists? file-candidate)
        (do (println file-candidate "exists") dir)
        (when-not (str/blank? dir) (recur (str (fs/parent dir))))))))

;; ********************************************************************************
;; copy

(defn copy-action
  "Copy a file at `path` from `src-dir` to `dst-dir`. The `options` could be `:replace-existing` `:copy-attributes`."
  ([path src-dir dst-dir options]
   (let [rp (build-filename/relativize path src-dir)
         path-on-disk (path-on-disk path)
         {:keys [file?]} path-on-disk]
     (assoc path-on-disk
            :src-dir src-dir
            :dst-dir dst-dir
            :options (merge {:replace-existing true
                             :copy-attributes true}
                            options)
            :relative-path rp
            :target-path (cond->> rp
                           file? build-filename/extract-path
                           :else (build-filename/create-file-path dst-dir)))))
  ([path src-dir dst-dir] (copy-action path src-dir dst-dir {})))

(defn do-copy-action
  "Do the actual copy of `copy-actions`, enrich `copy-action with `:status` (`:failed` or `:success`)"
  [{:keys [file? directory? path target-dir-path options exist?]
    :as copy-action}]
  (ensure-dir-exists target-dir-path)
  (cond-> copy-action
    (and exist? file?) (merge (try (copy-file path target-dir-path options)
                                   {:status :success}
                                   (catch Exception e
                                     {:status :failed
                                      :exception e})))
    (and exist? directory?) (merge (try (copy-dir path target-dir-path options)
                                        {:status :success}
                                        (catch Exception e
                                          {:status :failed
                                           :exception e})))))

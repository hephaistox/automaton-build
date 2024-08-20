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

(defn create-temp-file
  "Create a temporary file with `filename` name of the file (optional)."
  [& filename]
  (-> (fs/create-temp-file {:suffix (apply str filename)})
      str))

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

(defn write-file
  "Write the text file `filename` with its `content`."
  [filename content]
  (spit filename content)
  filename)

(defn read-file
  "Read the file named `filename`.

   Returns:

  * `filename`
  * `raw-content` if file can be read.
  * `invalid?` to `true` whatever why.
  * `exception` if something wrong happened."
  [filename]
  (let [filename (str filename)]
    (try {:filename filename
          :dir (build-filename/extract-path filename)
          :raw-content (slurp filename)}
         (catch Exception e
           {:filename filename
            :exception e
            :invalid? true}))))

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
  (->> (map (comp :raw-content read-file) filenames)
       (apply str)
       (write-file target-filename)))

(defn ensure-dir-exists
  "Creates directory `dir` if not already existing."
  [path]
  (when-not (is-existing-dir? path) (fs/create-dirs path)))

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

(defn pp-file
  "Pretty print the file."
  [filename file-content]
  (->> (with-out-str (pp/pprint file-content))
       (spit filename)))

(defn delete-file
  "Deletes `filename` if exists."
  [filename]
  (when (is-existing-file? filename) (fs/delete filename) filename))

(defn delete-dir
  "Deletes `dir` if exists.
   Returns nil if the `dir` does not exists, its unix path otherwise."
  [dir]
  (when (is-existing-dir? dir) (fs/delete-tree dir)))

(defn delete-path
  "Deletes `path` if exists.
   Returns nil if the `dir` does not exists, its unix path otherwise."
  [path]
  (if (fs/directory? path) (delete-dir path) (delete-file path)))

(defn search-in-parents
  "Search `file-or-dir` in the parents directories of `dir`."
  [dir file-or-dir]
  (loop [dir (build-filename/absolutize dir)]
    (let [file-candidate (build-filename/create-file-path (str dir)
                                                          file-or-dir)]
      (if (fs/exists? file-candidate)
        (do (println file-candidate "exists") dir)
        (when-not (str/blank? dir) (recur (str (fs/parent dir))))))))

(defn make-executable
  "Make file `filename` executable."
  [filename]
  (when (is-existing-file? filename)
    (fs/set-posix-file-permissions filename (fs/str->posix "rwx------"))))

(defn file-rich-list
  "Returns, for each element in the `paths`, return a rich file description with :

  * with `path` copied here,
  * `:exists?` key check the existence of the file.

  * and one of:
    * for a directory, `:dir?` key is true,
    * for a file, `:file?` key is true."
  [paths]
  (->> paths
       (map (fn [path]
              {:path path
               :exists? (fs/exists? path)
               (cond
                 (fs/directory? path) :dir?
                 (fs/exists? path) :file?
                 :else :missing)
               true}))
       set))

(defn copy-actions
  "For all `file-rich-list`, adds a `relative-path` and a `target-dir-path`."
  [file-rich-list src-dir dst-dir options]
  (->> file-rich-list
       (map (fn [{:keys [path file?]
                  :as action}]
              (let [rp (build-filename/relativize path src-dir)]
                (assoc action
                       :src-dir src-dir
                       :options (merge {:replace-existing true
                                        :copy-attributes true}
                                       options)
                       :dst-dir dst-dir
                       :relative-path rp
                       :target-dir-path (cond->> rp
                                          file? build-filename/extract-path
                                          :else (build-filename/create-file-path
                                                 dst-dir))))))
       set))

(defn to-src-dst
  "Turns `copy-action` into a list of pairs containing the source file first and destination file then."
  [copy-actions]
  (->> copy-actions
       (filter :exists?)
       (mapv (fn [{:keys [file? dir? path target-dir-path]}]
               [(cond
                  file? path
                  dir? (str path build-filename/directory-separator))
                (cond
                  file? target-dir-path
                  dir? (str target-dir-path
                            build-filename/directory-separator))]))))

(defn copy-file [src-path dst-path options] (fs/copy src-path dst-path options))

(defn copy-dir
  [src-path dst-path options]
  (fs/copy-tree src-path dst-path options))

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

(defn actual-copy
  "Do the actual copy of `copy-actions`."
  [copy-actions]
  (doseq [{:keys [file? dir? path target-dir-path options]}
          (filter :exists? copy-actions)]
    (ensure-dir-exists target-dir-path)
    (cond
      file? (copy-file path target-dir-path options)
      dir? (copy-dir path target-dir-path options)
      :else nil))
  copy-actions)

(defn empty-dir
  "Empty the directory `path`."
  [path]
  (delete-dir path)
  (ensure-dir-exists path))

(defn create-sym-link
  "Creates a sym link to `target` linking to `path`."
  [path target]
  (fs/create-sym-link path target))

(ns automaton-build.os.files
  "Tools to manipulate local files

  By convention, a directory always ends up with `/`

  Is a proxy to babashka.fs tools"
  (:require
   [automaton-build.log               :as build-log]
   [automaton-build.utils.comparators :as build-utils-comparators]
   [babashka.fs                       :as fs]
   [clojure.string                    :as str]))

;; ***********************
;; Manipulate file path (need no access)
;; ***********************
(def directory-separator
  "Symbol to separate directories.
  Is usually `/` on linux based OS And `\\` on windows based ones"
  fs/file-separator)

(defn change-extension
  "Change the extension"
  [file-name new-extension]
  (str (fs/strip-ext file-name) new-extension))

(defn remove-trailing-separator
  "If exists, remove the trailing separator in a path, remove unwanted spaces either"
  [path]
  (let [path (str/trim path)]
    (if (= (str directory-separator) (str (last path)))
      (->> (dec (count path))
           (subs path 0)
           remove-trailing-separator)
      path)))

(defn remove-useless-path-elements
  "When generated, it may happen that paths are using useless elements, for instance:
  * `//` double separator
  * `/./` current directory used a subdir"
  [dir]
  (loop [dir dir]
    (when dir
      (let [old-dir dir
            dir (-> dir
                    (str/replace (str directory-separator directory-separator)
                                 (str directory-separator))
                    (str/replace (str directory-separator "." directory-separator)
                                 directory-separator))]
        (if (= old-dir dir) dir (recur dir))))))

(defn create-file-path
  "Creates a path with the list of parameters.
  Removes the empty strings, add needed separators"
  [& dirs]
  (if (empty? dirs)
    (build-log/warn "Invalid create file path with no file set")
    (-> (if (some? dirs)
            (->> dirs
                 (mapv str)
                 (filter #(not (str/blank? %)))
                 (mapv remove-trailing-separator)
                 (interpose directory-separator)
                 (apply str))
            "./")
        remove-useless-path-elements
        str)))

(defn create-dir-path
  "Creates a path with the list of parameters.
  Removes the empty strings, add needed separators, including the trailing ones"
  [& dirs]
  (let [dir (apply create-file-path dirs)] (if (str/blank? dir) "" (str dir directory-separator))))

(defn create-absolute-dir-path
  "Creates an absolute path with the list of parameters.
  Removes the empty strings, add needed separators, including the trailing ones"
  [& dirs]
  (str (apply create-file-path directory-separator dirs) directory-separator))

(defn match-extension?
  "Returns true if the filename match the at least one of the extensions
  Params:
  * `filename`
  * `extensions` list of extension represented as a string to be tested"
  [filename & extensions]
  (when-not (str/blank? filename)
    (some (fn [extension] (str/ends-with? filename extension)) extensions)))

(defn file-in-same-dir
  "Use the relative-name to create in file in the same directory than source-file"
  [source-file relative-name]
  (let [source-subdirs (fs/components source-file)
        subdirs (mapv str (if (fs/directory? source-file) source-subdirs (butlast source-subdirs)))
        new-name (conj subdirs relative-name)]
    (apply create-file-path new-name)))

(defn is-absolute?
  "Returns true if the path is absolute
  Params:
  * `filename`"
  [filename]
  (= (str directory-separator) (str (first filename))))

(defn extract-path
  "Extract if the filename is a file, return the path that contains it,
  otherwise return the path itself
  Params:
  * `filename`"
  [filename]
  (when-not (str/blank? filename)
    (if (or (fs/directory? filename) (= directory-separator (str (last filename))))
      filename
      (let [filepath (->> filename
                          fs/components
                          butlast
                          (mapv str))]
        (cond
          (= [] filepath) ""
          (is-absolute? filename) (apply create-dir-path directory-separator filepath)
          :else (apply create-dir-path filepath))))))

;; ***********************
;; Change files on disk
;; ***********************

(defn absolutize
  "Transform a file or dir name in an absolute path"
  [relative-path]
  (when relative-path (str (fs/absolutize relative-path))))

(defn compare-paths
  "Are the paths pointing to the same directory?
  Params:
  * `path1`
  * `path2`"
  [path1 path2]
  (= (absolutize path1) (absolutize path2)))

(defn- copy-files-or-dir-validate
  "Internal function to validate data for `copy-files-or-dir`"
  [files]
  (if (and (sequential? files) (every? #(or (string? %) (= java.net.URL (class %))) files))
    true
    (do (build-log/error-exception
         (ex-info "The `files` parameter should be a sequence of string or `java.net.URL`"
                  {:files files}))
        false)))

(defn copy-files-or-dir
  "Copy the files, even if they are directories to the target
  * `files` is a sequence of file or directory name, in absolute or relative form
  * `target-dir` is where files are copied to

  Non existing files or directory are skipped"
  [files target-dir]
  (when (copy-files-or-dir-validate files)
    (try (fs/create-dirs target-dir)
         (doseq [file files]
           (if (fs/exists? file)
             (do (build-log/trace "Copy from " file " to " target-dir)
                 (if (fs/directory? file)
                   (do (build-log/trace-format "Copy directory `%s` to `%s`"
                                               (absolutize file)
                                               (absolutize target-dir))
                       (fs/copy-tree file
                                     target-dir
                                     {:replace-existing true
                                      :copy-attributes true}))
                   (do (build-log/trace-format "Copy file `%s` to `%s`"
                                               (absolutize file)
                                               (absolutize target-dir))
                       (fs/copy file
                                target-dir
                                {:replace-existing true
                                 :copy-attributes true}))))
             (build-log/debug-format "File `%s` was skipped as it is not existing" file)))
         true
         (catch Exception e
           (build-log/error-exception (ex-info "Unexpected exception during copy"
                                               {:exception e
                                                :files files
                                                :target-dir target-dir}))
           false))))

(defn delete-files
  "Deletes the files which are given in the list.
  They could be regular files or directory, when so the whole subtreee will be removed"
  [file-list]
  (doseq [file file-list]
    (when (fs/exists? file)
      (if (fs/directory? file)
        (do (build-log/trace "Directory " (absolutize file) " is deleted") (fs/delete-tree file))
        (do (build-log/trace "File " (absolutize file) " is deleted")
            (fs/delete-if-exists file))))))

(defn modified-since
  "Return true if anchor is older than one of the file in file-set"
  [anchor file-set]
  (let [file-set (filter some? file-set)] (when anchor (seq (fs/modified-since anchor file-set)))))

(defn current-dir
  "Return current dir"
  []
  (-> (fs/cwd)
      (str directory-separator)))

(defn relativize
  "Turn the `path` into a relative directory starting from `root-dir`"
  [path root-dir]
  (let [path (-> path
                 remove-trailing-separator
                 absolutize)
        root-dir (-> root-dir
                     remove-trailing-separator
                     absolutize)]
    (when-not (str/blank? root-dir)
      (->> path
           (fs/relativize root-dir)
           str))))

(defn relativize-to-pwd
  "Change the filename so it's now told relatively from current dir"
  [filename]
  (when filename
    (let [res (str/replace filename
                           (-> (current-dir)
                               re-pattern)
                           "")]
      (if (str/blank? res) "." res))))

(defn directory-exists?
  "Check directory existance"
  [directory-path]
  (and (fs/exists? directory-path) (fs/directory? directory-path)))

(defn ensure-directory-exists
  "Check the directory `dir` existance
  Returns nil if the creation has failed,
  Returns the `dir` otherwise

  Params:
  * `dir` directory to check if the directories exist"
  [dir]
  (try (when-not (directory-exists? dir)
         (build-log/trace-format "Directory %s is created" dir)
         (fs/create-dirs dir))
       (when (directory-exists? dir) dir)
       (catch Exception e (build-log/trace-exception e) nil)))

(defn is-existing-file?
  "Check if this the path exist and is not a directory
  Params:
  * `filename` the file to check
  Returns `filename` if it is an existing file, nil otherwise"
  [filename]
  (when-not (str/blank? filename)
    (when (and (fs/exists? filename) (not (fs/directory? filename))) filename)))

(defn filter-to-existing-files
  "Check if this the path exist and is not a directory
  Params:
  * `filename` the file to check
  Returns `filename` if it is an existing file, nil otherwise"
  [& filenames]
  (-> (filter (fn [filename]
                (if (is-existing-file? filename)
                  filename
                  (do (build-log/warn-format "File `%s` is filtered" filename) false)))
              filenames)
      vec))

(defn is-existing-dir?
  "Check if this the path exist and is a directory"
  [path]
  (and (fs/exists? path) (fs/directory? path)))

(defn create-dirs
  "Create a directory
  Returns the directory if ok
  Params:
  * `dir` directory to create"
  [dir]
  (if (is-existing-file? dir)
    (build-log/warn-format "Can't create a directory `%s` as a file already exists with that name"
                           (absolutize dir))
    (if (fs/exists? dir)
      (build-log/debug-format "Directory creation is skipped as the directory `%s` already exists"
                              dir)
      (try (fs/create-dirs dir)
           dir
           (catch Exception e
             (build-log/warn-exception (ex-info (format "Directory creation has failed `%s`"
                                                        (absolutize dir))
                                                {:dir dir}
                                                e)))))))

(defn overwrite-dirs
  "Creates a directory, if directory extists removes it first."
  [dir]
  (let [dir (absolutize dir)]
    (when (is-existing-dir? dir) (delete-files [dir]))
    (create-dirs dir)))

(defn search-files
  "Search files.
  * `root` is where the root directory of the search-files
  * `pattern` is a regular expression or a glob as described in [java doc](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
  * `options` (Optional, default = {}) are boolean value for `:hidden`, `:recursive` and `:follow-lins`. See [babashka fs](https://github.com/babashka/fs/blob/master/API.md#glob) for details.
  For instance:
  * `(files/search-files \"\" \"**{.clj,.cljs,.cljc,.edn}\")` search all clj files in pwd directory"
  ([root pattern options]
   (if (directory-exists? root)
     (mapv str
           (fs/glob root
                    pattern
                    (merge {:hidden true
                            :recursive true
                            :follow-links true}
                           options)))
     (do (build-log/warn-format "Search aborted as `%s` is not a directory" root)
         (build-log/trace-map "search parameters" :root root :pattern pattern :options options)
         [])))
  ([root pattern] (search-files root pattern {})))

(defn read-file
  "Read the file `target-filename`
  Options:
  * `target-filename` name of the file to change
  * `silently` (Optional, default= nil) if `:silently`, won't tell in the log"
  ([target-filename silently?]
   (when-not (= :silently silently?) (build-log/trace-format "Reading file `%s`" target-filename))
   (try (slurp (-> target-filename
                   fs/expand-home
                   str))
        (catch Exception e (build-log/error-exception e) nil)))
  ([target-filename] (read-file target-filename :silently)))

(defn expand-home "Expand home parameter" [dir] (fs/expand-home dir))

(defn create-parent-dirs
  "Ensures the parents directory exist, so file saving will be sucessful
  Params:
  * `filename`"
  [filename]
  (let [filepath (extract-path filename)]
    (try (when-not (str/blank? filepath) (fs/create-dirs filepath))
         (catch Exception e
           (build-log/error-exception (ex-info "Impossible to create directory"
                                               {:caused-by e
                                                :filename filename
                                                :filepath filepath}))))))

(defn spit-file
  "Spit the file and return the content. Return false if the file didn't change, return nil if there is an error during spit.
  Params:
  * `filename` is the name of the file to write, could be absolute or relative
  * `content` is the content to store there
  * `header`(optional) header that should be added to file (e.g. 'File automatically modified, do not edit')
  * `compare-fn` (optional - default: compare-changes) function to compare changes when file exists in the target `filename`. Expects to return boolean to tell if `content` and target `filename` content is the same. Accepts three arguments, `filename`, `content`, `header`. The simplest implementation to skip comparing would be (fn [_filename _content _header] false).
  * `writer` (optional - default: spit) function to spit the file"
  ([filename content header compare-fn writer]
   (try
     (build-log/trace-format "Writing file `%s`" filename)
     (let [filename (absolutize filename)]
       (when (nil? filename)
         (throw (ex-info "Impossible to save the file due to empty filename" {:filename filename})))
       (let [content-with-header
             (if (str/blank? (str header)) content (str (with-out-str (println header)) content))]
         (cond
           (and (is-existing-file? filename) (compare-fn filename content header))
           (do (build-log/trace-format "Spit of file `%s` skipped, as it is already up to date:"
                                       filename)
               false)
           (some? content)
           (do (build-log/trace-format "Spit of file `%s` is updating with new content." filename)
               (create-parent-dirs filename)
               (writer filename content-with-header)
               content)
           :else (throw (ex-info "Content of a file is empty"
                                 {:filename filename
                                  :content content})))))
     (catch Exception e
       (build-log/error-data {:filename filename
                              :exception e
                              :content content}
                             (format "Impossible to update the `%s` file." filename))
       nil)))
  ([filename content header compare-fn] (spit-file filename content header compare-fn spit))
  ([filename content header]
   (spit-file filename
              content
              header
              (partial build-utils-comparators/compare-file-change read-file)
              spit))
  ([filename content]
   (spit-file filename
              content
              nil
              (partial build-utils-comparators/compare-file-change read-file)
              spit)))

(defn create-temp-dir
  "Creates a temporary directory managed by the system
  Params:
  * `sub-dirs` is an optional list of strings, each one is a sub directory
  Returns the string of the directory path"
  [& sub-dirs]
  (let [tmp-dir (apply create-dir-path
                       (-> (fs/create-temp-dir)
                           str)
                       (interpose directory-separator sub-dirs))]
    (ensure-directory-exists tmp-dir)))

(defn filter-existing-dir
  "Filter only existing dirs
  Params:
  * `dirs` sequence of string of directories"
  [dirs]
  (apply vector
         (mapcat (fn [sub-dir]
                   (let [sub-dir-rpath (absolutize sub-dir)]
                     (when (directory-exists? sub-dir-rpath) [sub-dir-rpath])))
          dirs)))

(defn filename "Return the file name without the path" [path] (fs/file-name path))

(defn sorted-absolutize-dirs
  "Create a proper list of absolutized deduped directories
  Params:
  * `base-dir` is where the relative dirs will start
  * `relative-dirs` are the relative directories that needed to be absolutized"
  [base-dir & relative-dirs]
  (->> relative-dirs
       (mapv (comp absolutize (partial create-dir-path base-dir)))
       dedupe
       sort
       (into [])))

(defn for-each
  "Apply fn-each on each files in a directory
  Params:
  * `dir` where to start the search
  * `file-pattern` is the pattern to match files, could be a glob
  * `fn-each` function likely with side effects that will be applied on each file name"
  [dir file-pattern fn-each]
  (doseq [file (fs/glob dir file-pattern)] (fn-each (str file))))

(defn create-temp-file
  "Create a temporary file
  Params:
  * `filename` name of the file (optional)"
  [& filename]
  (-> (fs/create-temp-file {:suffix (apply str filename)})
      str))

(defn search-in-parents
  "Search in parents directories
  Params:
  * `dir` starting point for the search
  * `file-or-dir` file or directory to search for"
  [dir file-or-dir]
  (loop [dir (absolutize dir)]
    (let [file-candidate (create-file-path (str dir) file-or-dir)]
      (if (fs/exists? file-candidate)
        dir
        (when-not (str/blank? dir) (recur (str (fs/parent dir))))))))

(defn make-executable
  "Make a file executable"
  [filename]
  (when (is-existing-file? filename)
    (fs/set-posix-file-permissions filename (fs/str->posix "rwx------"))))

(defn parent-dirs-of-files
  "Returns sequence of absolute parent directory paths of files.
   e.g. [\"a/b/c.clj\"] -> [\"Users/Mati/a/b/\"]
   Params:
   * files - sequence of strings with pathnames"
  [files]
  (mapv (comp str fs/parent fs/absolutize) files))

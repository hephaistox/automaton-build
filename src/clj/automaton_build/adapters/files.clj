(ns automaton-build.adapters.files
  "Tools to manipulate local files
  Is a proxy to babashka.fs tools"
  (:require
   [clojure.string :as str]

   [babashka.fs :as fs]

   [automaton-build.adapters.log :as log]
   [automaton-build.adapters.uuid :as uuid]
   [automaton-build.env-setup :as env-setup]))

(def file-separator
  "Symbol to separate directories.
  Is usually `/` on linux based OS And `\\` on windows based ones
  For internal use only, consider `create-dir-path` or `create-file-path` to assemble directories"
  fs/file-separator)

(defn absolutize
  "Transform a file or dir name in an absolute path"
  [relative-path]
  (when relative-path
    (str (fs/absolutize relative-path))))

(defn file-name
  "Return the file name without the path"
  [path]
  (fs/file-name path))

(defn delete-files
  "Deletes the files which are given in the list.
  They could be regular files or directory, when so the whole subtreee will be removed"
  [file-list]
  (doseq [file file-list]
    (if (fs/directory? file)
      (do
        (log/debug "Directory " (absolutize file) " is deleted")
        (fs/delete-tree file))
      (do
        (log/debug "File " (absolutize file) " is deleted")
        (fs/delete-if-exists file)))))

(defn- copy-files-or-dir-validate
  "Internal function to validate data aof copy files or dir"
  [files]
  (when-not (and (sequential? files)
                 (every? #(or (string? %)
                              (= java.net.URL
                                 (class %)))
                         files))
    (throw (ex-info "The `files` parameter should be a sequence of string or `java.net.URL`"
                    {:files files}))))

(defn copy-files-or-dir
  "Copy the files, even if they are directories to the target
  * `files` is a sequence of file or directory name, in absolute or relative form
  * `target-dir` is where files are copied to"
  [files target-dir]
  (log/debug "Copy files from `" files "` to `" target-dir "`")
  (copy-files-or-dir-validate files)
  (try
    (fs/create-dirs target-dir)
    (doseq [file files]
      (log/debug "Copy from " file " to " target-dir)
      (if (fs/directory? file)
        (do
          (log/trace (format "Copy `%s` to `%s`"
                             (absolutize file)
                             (absolutize target-dir)))
          (fs/copy-tree file target-dir
                        {:replace-existing true
                         :copy-attributes true}))
        (do
          (log/trace (format "Copy `%s` to `%s`"
                             (absolutize file)
                             (absolutize target-dir)))
          (fs/copy file target-dir
                   {:replace-existing true
                    :copy-attributes true}))))
    (catch Exception e
      (throw (ex-info "Unexpected exception during copy"
                      {:exception e
                       :files files
                       :target-dir target-dir})))))

(defn directory-exists?
  "Check directory existance"
  [directory-path]
  (and (fs/exists? directory-path)
       (fs/directory? directory-path)))

(defn is-existing-file?
  "Check if this the path exist and is not a directory"
  [path]
  (and (fs/exists? path)
       (not (fs/directory? path))))

(defn is-existing-dir?
  "Check if this the path exist and is a directory"
  [path]
  (and (fs/exists? path)
       (fs/directory? path)))

(defn create-dirs
  "Create a directory"
  [dir]
  (when (is-existing-file? dir)
    (throw (ex-info (format "Can't create a directory `%s` as a file already exists with that name"
                            (absolutize dir))
                    {:dir dir})))
  (when-not (fs/exists? dir)
    (try
      (fs/create-dirs dir)
      (catch Exception e
        (throw (ex-info (format "The parameter is not a valid directory: `%s`" dir)
                        {:dir (absolutize dir)
                         :exception e})))))
  true)

(defn remove-trailing-separator
  "If exists, remove the trailing separator in a path, remove unwanted spaces either"
  [path]
  (let [path (str/trim path)]
    (str/replace path (re-pattern (str file-separator "*$")) "")))

(defn create-file-path
  "Creates a path with the list of parameters.
  Removes the empty strings, add needed separators"
  [& dirs]
  (if (some? dirs)
    (do
      (when-not (every? string?
                        dirs)
        (throw (ex-info "The directories can't be casted in string"
                        {:dirs dirs})))
      (->> dirs
           (filter #(not (str/blank? %)))
           (map remove-trailing-separator)
           (interpose file-separator)
           (apply str)))
    "."))

(defn create-dir-path
  "Creates a path with the list of parameters.
  Removes the empty strings, add needed separators, including the trailing ones"
  [& dirs]
  (when-let [file-path (apply create-file-path dirs)]
    (str file-path file-separator)))

(defn search-files
  "Search files.
  * `root` is where the root directory of the search-files
  * `pattern` is a regular expression as described in [java doc](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
  * `options` (Optional, default = {}) are boolean value for `:hidden`, `:recursive` and `:follow-lins`. See [babashka fs](https://github.com/babashka/fs/blob/master/API.md#glob) for details.
  For instance:
  * `(files/search-files \"\" \"**{.clj,.cljs,.cljc,.edn}\")` search all clj files in pwd directory"
  ([root pattern options]
   (when-not (directory-exists? root)
     (throw (ex-info (str root " is expected to be a directory") {:root root
                                                                  :pattern pattern
                                                                  :options options})))
   (into []
         (map str
              (fs/glob root pattern (merge {:hidden true
                                           :recursive true
                                            :follow-links true}
                                           options)))))
  ([root pattern]
   (search-files root pattern {})))

(defn list-subdir
  "List subdirectories"
  [root]
  (->> (fs/list-dir root fs/directory?)
       (map str)))

(defn file-prefix
  "Adds `file:` as a prefix to the string. Usefull when java io resource type of path is needed"
  [path]
  (str "file:" path))

(defn for-each
  "Apply fn-each on each files in a directory"
  [dir fn-each]
  (doseq [file (fs/list-dir dir)]
    (fn-each (str file))))

(defn change-extension
  "Change the extension"
  [file-name new-extension]
  (str (fs/strip-ext file-name)
       new-extension))

(defn modified-since
  "Return true if anchor is older than one of the file in file-set"
  [anchor file-set]
  (fs/modified-since anchor file-set))

(defn file-in-same-dir
  "Use the relative-name to create in file in the same directory than source-file"
  [source-file relative-name]
  (let [source-subdirs (fs/components source-file)
        subdirs (mapv str
                      (if (fs/directory? source-file)
                        source-subdirs
                        (butlast source-subdirs)))
        new-name (conj subdirs relative-name)]
    (apply create-file-path new-name)))

(defn extract-path
  "Extract if the filename is a file, return the path that contains it,
  otherwise return the path itself"
  [filename]
  (when-not (str/blank? filename)
    (if (fs/directory? filename)
      filename
      (str
       (when (= (str file-separator)
                (str (first filename)))
         file-separator)
       (->> filename
            fs/components
            butlast
            (map str)
            (apply create-dir-path))))))

(defn rename-file
  "Rename a file `src` to destination file `dst`"
  [src dst]
  (try
    (-> dst
        extract-path
        fs/create-dirs)

    (log/trace "Rename file " src " to " dst)
    (fs/move src (fs/path dst))
    (catch Exception e
      (throw (ex-info "Impossible to rename the file"
                      {:src (absolutize src)
                       :dst (absolutize dst)
                       :exception e})))))

(defn rename-dir
  "Rename a dir"
  [src dst]
  (try
    (log/trace "Rename directory " src " to " dst)
    (fs/move src (fs/path dst))
    (catch Exception e
      (throw (ex-info "Impossible to rename the directory"
                      {:src (absolutize src)
                       :dst (absolutize dst)
                       :exception e})))))

(defn remove-file
  "Remove the file `filename`"
  [filename]
  (try
    (fs/delete-if-exists filename)
    (catch Exception e
      (throw (ex-info "Impossible to remove the file"
                      {:filename filename
                       :exception e})))))

(defn read-file
  "Read the file `target-filename`"
  [target-filename]
  (log/trace (str "Reading file `" target-filename "`"))
  (try
    (slurp target-filename)
    (catch Exception e
      (throw (ex-info "Impossible to load the file"
                      {:target-filename target-filename
                       :exception e})))))

(defn file-ized
  "Transform a name, like a namespace name or application name, in a directory compatible names"
  [namespace]
  (str/replace namespace #"-" "_"))

(defn add-suffix
  "Add a suffix of the filename before the extension"
  [filename suffix]
  (let [[_ prefix extension] (re-find #"(.*)(\..*)" filename)]
    (str/join [prefix suffix extension])))

(defn- rename-recursively-attempt
  "Make one attempt for file renaming.
  Search for all files matching target-dir and file-pattern
  Will stop at the first modification
  Return modification? telling if at least one renaming has been done"
  [target-dir file-filter pattern pattern-replacement]
  (loop [files (search-files target-dir
                             file-filter)
         modification? false]
    (if (empty? files)
      modification?
      (let [filename (str (first files))
            new-filename (str/replace filename
                                      pattern
                                      pattern-replacement)]
        (if (= new-filename filename)
          (recur (rest files) modification?)
          (cond
            (is-existing-file? filename) (do (rename-file filename new-filename)
                                             (recur (rest files) true))
            (is-existing-dir? filename) (do (rename-dir filename new-filename)
                                            (recur (rest files) true))
            :else (recur (rest files) modification?)))))))

(defn rename-recursively
  "Search recursively all sub-dirs to be renamed from `template-app` in the `target-dir` directory
  * `target-dir` is the root directory of the searched files
  * `file-filter` filter for files, [according to syntax in crate regexp](https://docs.rs/regex/1.9.1/regex/#syntax).
  * `pattern` is to find content in the namespace, for instance, as seen in [java pattern](https://docs.oracle.com/javase/10/docs/api/java/util/regex/Pattern.html), e.g. #\"foo(.*)bar\"
  * `pattern-replacement` is what to replace, according to the [replace specification](https://clojuredocs.org/clojure.string/replace). e.g. \"foo_$1_bar\" "
  [target-dir file-filter pattern pattern-replacement]
  (log/debug "Rename files and directories in " target-dir " , from `" pattern "` to `" pattern-replacement "`")
  (let [pattern (remove-trailing-separator (str pattern))
        pattern-replacement (remove-trailing-separator (str pattern-replacement))]
    (loop [iterations-left 30]
      (if (> iterations-left 0)
        (when (rename-recursively-attempt target-dir file-filter pattern pattern-replacement)
          (recur (dec iterations-left)))
        (log/warn "Infinite loop detected during renaming")))))

(defn create-files-map
  "Return the files in `target-dir`, matching the `pattern`.
  A map is built with the filename as a key, and the content as a value
  * `target-dir` is where the files with be searched at,
  * `pattern` is a regular expression as described in [java doc](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)) "
  [target-dir pattern]
  (log/debug "Create files-map for " target-dir " with parameter " pattern)
  (->> (search-files target-dir
                     pattern)
       (filter is-existing-file?)
       (map (fn [filename]
              [(str (absolutize filename))
               (read-file (absolutize filename))]))
       (into {})))

(defn delete-files-starting-with
  "Remove all files matching:
  * `files-map` where the files are searched
  * `starting-regexp` is a regular expression matching the first line of the file "
  [files-map starting-regexp]
  (log/debug "Delete files starting with " starting-regexp)
  (doseq [[filename file-content] files-map]
    (let [first-line (-> (str/split-lines file-content)
                         first)]
      (when (re-find starting-regexp first-line)
        (remove-file filename)
        (log/trace "File" filename " has been removed")))))

(defn spit-file
  "Spit the file, the directory where to store the file is created if necessary
  * `filename` is the name of the file to write, could be absolute or relative
  * `content` is the content to store there"
  [filename content]
  (let [filepath (extract-path filename)]
    (fs/create-dirs filepath)
    (spit filename content)))

(defn create-temp-dir
  "Creates a temporary directory
  The directory and its parents are created,
  Params:
  * none
  Returns the string of the directory path"
  []
  (let [tmp-dir (apply create-dir-path [(get-in env-setup/env-setup
                                                [:tests :tmp-dirs])
                                        (str (uuid/time-based-uuid))])]
    (fs/create-dirs tmp-dir)
    tmp-dir))

(defn filter-existing-dir
  "Filter only existing dirs
  Params:
  * `dirs` sequence of string of directories"
  [dirs]
  (apply vector
         (mapcat (fn [sub-dir]
                   (let [sub-dir-rpath (absolutize sub-dir)]
                     (when (directory-exists? sub-dir-rpath)
                       [sub-dir-rpath])))
                 dirs)))

(defn empty-path?
  "Is the directory empty
  Params:
  * `dir` the directory to search in"
  [dir]
  (boolean
   (and (fs/directory? dir)
        (empty? (fs/list-dir dir)))))

(ns automaton-build.headers.files
  "Read files with headers log."
  (:require
   [automaton-build.echo.headers   :refer [errorln exceptionln normalln uri-str]]
   [automaton-build.os.cmd         :refer [as-string]]
   [automaton-build.os.edn-utils   :as build-edn]
   [automaton-build.os.file        :as build-file]
   [automaton-build.os.filename    :as build-filename]
   [automaton-build.project.config :as build-project-config]
   [clojure.string                 :as str]))

;; ********************************************************************************
;; File reading

(defn read-file-quiet "Read a file with no message." [filename] (build-file/read-file filename))

(defn read-file-if-error
  "Read a file and prints error if it is not loaded properly."
  [filename]
  (let [file-desc (read-file-quiet filename)
        {:keys [invalid? filename exception]} file-desc]
    (when invalid?
      (errorln "File" filename " is not loaded.")
      (when exception (normalln "This exception has raised") (exceptionln exception)))
    file-desc))

(defn read-file
  "Read a file, print it, and if it is not loaded properly."
  [filename]
  (normalln (str "Read file `" (build-file/expand-home-str filename) "`"))
  (read-file-if-error filename))

;; ********************************************************************************
;; edn reading

(defn read-edn-quiet "Read a file with no message." [filename] (build-edn/read-edn filename))

(defn read-edn-if-error
  "Read a file and prints error if it is not loaded properly."
  [filename]
  (let [file-desc (read-edn-quiet filename)
        {:keys [invalid? filename exception]} file-desc]
    (when invalid?
      (errorln "File" filename " is not loaded.")
      (when exception (normalln "This exception has raised") (exceptionln exception)))
    file-desc))

(defn read-edn
  "Read a file, print it, and if it is not loaded properly."
  [filename]
  (normalln (str "Read file `" (build-file/expand-home-str filename) "`"))
  (read-edn-if-error filename))

;; ********************************************************************************
;; project configuration

(defn project-config-quiet
  "Returns the project configuration in `app-dir`."
  [app-dir]
  (build-project-config/read-from-dir app-dir))

(defn project-config-if-error
  [app-dir]
  (let [file-desc (project-config-quiet app-dir)
        {:keys [invalid? filename exception]} file-desc]
    (when invalid?
      (errorln "Impossible to find project-dir in" (uri-str filename))
      (exceptionln exception))
    file-desc))

(defn project-config
  "Returns the project configuration in `app-dir`."
  [app-dir]
  (project-config-if-error app-dir))

;; ********************************************************************************
;; search, move and copy files

(defn search-files
  "Search files in `root-dir`"
  ([root-dir filters] (build-file/search-files root-dir filters))
  ([root-dir filters options] (build-file/search-files root-dir filters options)))

(defn- dir
  "Returns the path as a string, whatever `path` is a `URL` or a `string`."
  [path]
  (if (string? path) path (.getFile path)))

(defn copy-files
  "Copy files from `src-dir` to `dst-dir` applying the `filters`."
  [src-dir dst-dir filters verbose options]
  (let [src-dir (dir src-dir)
        dst-dir (dir dst-dir)
        copy-actions (-> src-dir
                         str
                         (build-file/search-files filters)
                         build-file/file-rich-list
                         (build-file/copy-actions src-dir dst-dir options))]
    (when verbose
      (normalln "Copy (from) -> (to)")
      (normalln (->> copy-actions
                     build-file/to-src-dst
                     (mapv (fn [[s d]] (str (uri-str s) "->" (uri-str d))))
                     (str/join ",")))
      (let [removed-files (remove :exists? copy-actions)]
        (when-not (empty? removed-files)
          (errorln "These files are not found and excluded from the copy:"
                   (str/join "," (mapv :path removed-files))))))
    (build-file/actual-copy copy-actions)))

(defn create-sym-link-quiet
  "Creates a sym link called `target` toward `path`.

  The link will be stored relatively to `base-dir`"
  [path target base-dir]
  (merge {:base-dir base-dir
          :path path
          :target target}
         (if-not (build-file/is-existing-dir? base-dir)
           {:status :base-dir-doesnt-exist}
           (try (let [path (build-filename/relativize path base-dir)
                      target (build-filename/relativize target base-dir)
                      res (as-string ["ln"
                                      "-s"
                                      (->> (build-filename/extract-path target)
                                           (build-filename/relativize path))
                                      target]
                                     base-dir)]
                  res)
                (catch Exception e (exceptionln e) nil)))))

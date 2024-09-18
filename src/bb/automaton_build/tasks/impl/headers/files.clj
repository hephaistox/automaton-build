(ns automaton-build.tasks.impl.headers.files
  "Read files with headers log."
  (:require
   [automaton-build.echo.headers            :refer [errorln exceptionln normalln uri-str]]
   [automaton-build.os.edn-utils-bb         :as build-edn]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.project.config          :as build-project-config]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd success]]
   [clojure.string                          :as str]))

(defn project-config
  "Returns the project configuration in `app-dir`."
  [app-dir]
  (let [res (-> app-dir
                build-project-config/read-from-dir)
        {:keys [invalid? filename exception]} res]
    (when invalid?
      (errorln "Impossible to find project-dir in" (uri-str filename))
      (exceptionln exception))
    res))

(defn invalid-project-name-message
  "Print the error message to tell the specified project was wrong."
  [monorepo-project-map]
  (let [project-example-name (-> monorepo-project-map
                                 :subprojects
                                 first
                                 :app-name)]
    (errorln "Invalid project name, please specify with \"-p\""
             "(e.g. `bb -p"
             project-example-name
             "`).")
    (normalln "Choose among:"
              (str/join ", "
                        (map pr-str
                             (remove nil?
                                     (map (fn [subproject] (:app-name subproject))
                                          (:subprojects monorepo-project-map))))))))

(defn print-file-errors
  "Print errors for a text file not being loaded.

  Returns true if an error is found."
  [{:keys [invalid? filename exception]
    :as _file-desc}]
  (when invalid?
    (errorln "File" filename " is not loaded.")
    (when exception (normalln "This exception has raised") (exceptionln exception)))
  invalid?)

(defn read-file
  "Read a file and prints error if it is not loaded properly."
  [filename]
  (let [file-desc (build-file/read-file filename)]
    (print-file-errors file-desc)
    (:raw-content file-desc)))

(defn print-edn-errors
  "Print errors for an edn file not being loaded.

   Returns true if an error is found."
  [{:keys [raw-content]
    :as file-desc}]
  (let [r (print-file-errors file-desc)]
    (when r (normalln "Raw content is:\n" raw-content))
    r))

(defn read-edn-file
  "Read an edn file and prints error if it is not loaded properly."
  [filename]
  (let [file-desc (build-edn/read-edn filename)]
    (print-file-errors file-desc)
    (:edn file-desc)))

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

(defn create-sym-link
  "Creates a sym link called `target` toward `path`.

  The link will be stored relatively to `base-dir`"
  [path target base-dir]
  (if-not (build-file/is-existing-dir? base-dir)
    (do (errorln "The symlink should have a directory as a base-dir") nil)
    (try (let [path (build-filename/relativize path base-dir)
               target (build-filename/relativize target base-dir)
               res (blocking-cmd ["ln"
                                  "-s"
                                  (->> (build-filename/extract-path target)
                                       (build-filename/relativize path))
                                  target]
                                 base-dir
                                 "Soft symbol link creation has failed."
                                 false)]
           (success res))
         (catch Exception e (exceptionln e) nil))))

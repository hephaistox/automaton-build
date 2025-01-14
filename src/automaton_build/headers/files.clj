(ns automaton-build.headers.files
  "Read files with headers log."
  (:require
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
  [{:keys [errorln normalln exceptionln]
    :as _printers}
   filename]
  (let [file-desc (read-file-quiet filename)
        {:keys [status filename exception]} file-desc]
    (when-not (= :success status)
      (errorln "File" filename " is not loaded.")
      (when exception (normalln "This exception has raised") (exceptionln exception)))
    file-desc))

(defn read-file
  "Read a file, print it, and if it is not loaded properly."
  [{:keys [normalln]
    :as printers}
   filename]
  (normalln (str "Read file `" (build-file/expand-home-str filename) "`"))
  (read-file-if-error printers filename))

;; ********************************************************************************
;; edn reading

(defn read-edn-quiet "Read a file with no message." [filename] (build-edn/read-edn filename))

(defn read-edn-if-error
  "Read a file and prints error if it is not loaded properly."
  [{:keys [normalln errorln exceptionln]
    :as _printers}
   filename]
  (let [file-desc (build-edn/read-edn filename)
        {:keys [status filename exception]} file-desc]
    (when-not (= :success status)
      (errorln "File" filename " is not loaded.")
      (when exception (normalln "This exception has raised") (exceptionln exception)))
    file-desc))

(defn read-edn
  "Read a file, print it, and if it is not loaded properly."
  [{:keys [normalln]
    :as printers}
   filename]
  (normalln (str "Read file `" (build-file/expand-home-str filename) "`"))
  (read-edn-if-error printers filename))

;; ********************************************************************************
;; project configuration

(defn project-config-quiet
  "Returns the project configuration in `app-dir`."
  [app-dir]
  (build-project-config/read-from-dir app-dir))

(defn project-config-if-error
  [{:keys [errorln uri-str exceptionln]
    :as _printers}
   app-dir]
  (let [file-desc (project-config-quiet app-dir)
        {:keys [status filename exception]} file-desc]
    (when-not (= :success status)
      (errorln "Impossible to find project-dir in" (uri-str filename))
      (exceptionln exception))
    file-desc))

(defn project-config
  "Returns the project configuration in `app-dir`."
  [printers app-dir]
  (project-config-if-error printers app-dir))

;; ********************************************************************************
;; search, move and copy files

(defn search-files
  "Search files in `root-dir`"
  ([root-dir filters] (build-file/search-files root-dir filters))
  ([root-dir filters options] (build-file/search-files root-dir filters options)))

(defn copy-files
  "Copy files from `src-dir` to `dst-dir` applying the `filters`."
  [{:keys [errorln normalln]
    :as _printers}
   src-dir
   dst-dir
   filters
   verbose
   options]
  (when verbose (normalln "Copy `" src-dir "` -> `" dst-dir "`"))
  (let [errors (->> (build-file/search-files (str src-dir) filters options)
                    (map (fn [file-path]
                           (let [copy-action (-> file-path
                                                 (build-file/copy-action src-dir dst-dir))
                                 copy-action (build-file/do-copy-action copy-action)]
                             (when verbose (normalln (str "Copy `" src-dir "` -> `" dst-dir "`")))
                             copy-action)))
                    (group-by :status))
        {:keys [success failure skipped]} errors]
    (when (and verbose success)
      (errorln "These copies have been copied:")
      (doseq [{:keys [apath target-path]} failure] (normalln apath "->" target-path)))
    (when failure
      (errorln "These copies have failed")
      (doseq [{:keys [apath target-path]} failure]
        (normalln apath
                  "->" target-path
                  ": " (str/join ","
                                 (pr-str (select-keys failure
                                                      [:type :exist? :target-path :options]))))))
    (when skipped
      (errorln "These files are skipped")
      (doseq [{:keys [apath target-path]} failure]
        (normalln apath
                  "->" target-path
                  ": " (str/join ","
                                 (pr-str (select-keys failure
                                                      [:type :exist? :target-path :options]))))))))

(defn create-sym-link-quiet
  "Creates a sym link called `target` toward `path`.

  The link will be stored relatively to `base-dir`"
  [{:keys [exceptionln]
    :as _printers}
   path
   target
   base-dir]
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

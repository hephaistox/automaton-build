(ns automaton-build.wf.tests
  "Common tasks fn for testing."
  (:require
   [automaton-build.app-data.deps :as build-deps]
   [automaton-build.code.files    :as build-code-files]
   [automaton-build.code.lint     :as build-code-lint]
   [automaton-build.code.reports  :as build-code-reports]
   [automaton-build.echo.headers  :as build-echo-headers]
   [automaton-build.os.cmds       :as build-commands]
   [automaton-build.os.file       :as build-file]
   [automaton-build.wf.common     :as build-wf-common]
   [clojure.string                :as str]))

(def commit-message ["-c" "--commit-message" "Commit message"])

(def lint-opts ["-L" "--lint" "Don't lint" :default true :parse-fn not])

(def reports-opts
  ["-r" "--reports" "Don't execute reports" :default true :parse-fn not])

(defn lint
  "Lint task."
  [deps cli-opts]
  (try (let [paths-to-lint (->> deps
                                build-deps/get-src
                                (keep build-file/is-existing-dir?))]
         (if (empty? paths-to-lint)
           {:message "Nothing to lint."}
           (let [lint-cmd (build-code-lint/lint-cmd false paths-to-lint)
                 _ (when (get-in cli-opts [:options :verbose])
                     (build-wf-common/print-cmd lint-cmd))
                 {:keys [out err cmd exit]} (build-commands/blocking-cmd
                                             lint-cmd)]
             (if (zero? exit)
               {:message "Linted."
                :status :ok}
               {:message (format "Failed linted command %s" cmd)
                :details (str err out)}))))
       (catch Exception e
         (build-echo-headers/errorln "Unexpected error: ")
         (build-echo-headers/exceptionln e)
         (Thread/sleep 1000))))

(defn- save-report
  [matches filename]
  (if (empty? matches)
    (build-file/delete-file filename)
    (build-file/pp-file filename matches)))

(defn reports
  "Reports the project with error in the code.
  `deps` is used to discover the files to search in.
  with `cli-opts`, `verbose` is triggered or not."
  [deps cli-opts]
  (try
    (let [dirs (build-code-files/project-dirs deps)
          files (build-code-files/project-files dirs)]
      (if (empty? files)
        {:message "Nothing to report on."}
        (do
          (doseq [{:keys [content filename]} files]
            (when (empty? content)
              (build-echo-headers/errorln "Skip file : "
                                          filename
                                          " is unexpectedly unreadable.")))
          (when (get-in cli-opts [:options :verbose])
            (->> (pr-str dirs)
                 (build-echo-headers/normalln "Dirs:")))
          (let [matches (->> files
                             (mapcat (fn [{:keys [filename content]}]
                                       (build-code-reports/search-aliases
                                        filename
                                        content)))
                             (remove (fn [{:keys [alias]}]
                                       (contains? #{"sut" nil} alias))))
                two-aliases (build-code-reports/ns-inconsistent-aliases matches)
                two-ns (build-code-reports/alias-inconsistent-ns matches)
                comment (->> files
                             (map (fn [{:keys [filename content]}]
                                    {:filename filename
                                     :match (build-code-reports/comments
                                             content)}))
                             (filterv (comp not empty? :match)))
                reports [{:res two-aliases
                          :filename "docs/code/ns-inconsistent-alias.edn"}
                         {:res two-ns
                          :filename "docs/code/alias-inconsistent-ns.edn"}
                         {:res comment
                          :filename "docs/code/comment.edn"}]
                failed-reports (->> reports
                                    (filter (comp not empty? :res)))]
            (doseq [{:keys [res filename]
                     :as _report}
                    reports]
              (save-report res filename))
            (if (empty? failed-reports)
              {:message (str "Reported "
                             (count files)
                             " files, "
                             (count matches)
                             " matches.")
               :status :ok}
              {:message (str "Reported errors ("
                             (count files)
                             " files, "
                             (count matches)
                             ") matches, found errors in ("
                             (str/join ","
                                       (->> failed-reports
                                            (keep (comp str :filename))
                                            (mapv build-echo-headers/uri)))
                             ").")})))))
    (catch Exception e
      (build-echo-headers/errorln "Unexpected error: ")
      (build-echo-headers/exceptionln e)
      (Thread/sleep 1000))))

(defn commit [_project-data _cli-opts] {:status :ok})

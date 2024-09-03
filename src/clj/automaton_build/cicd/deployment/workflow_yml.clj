(ns automaton-build.cicd.deployment.workflow-yml
  "Code related to GitHub Actions workflow YML file"
  (:require
   [automaton-build.log      :as build-log]
   [automaton-build.os.files :as build-files]
   [clojure.string           :as str]))

(defn- spit-workflow*
  [filename file-content searched-pattern tag]
  (let [new-content (str/replace file-content searched-pattern (str "$1" tag))]
    (when-not (nil? new-content) (build-files/spit-file filename new-content) true)))

(defn- searched-pattern
  [container-name]
  (-> (str "(uses:\\s*docker://\\w*/" container-name ")(.*)")
      re-pattern))

(defn slurp-tag
  "Returns tag used in workflow file
  Params:
  * `filename` filename to modify
  * `container-name` name of the container to update"
  [filename container-name]
  (if-let [file-content (some-> filename
                                build-files/is-existing-file?
                                build-files/read-file)]
    (let [searched-pattern (searched-pattern container-name)
          search-result (re-find searched-pattern file-content)]
      (last search-result))
    (do (build-log/warn-format "File %s doesn't exist, unable to show the container tag in it"
                               filename)
        false)))

(defn spit-workflow
  "Update a workflow files with
  Params:
  * `filename` filename to modify
  * `container-name` name of the container to update
  * `tag` tag to upsert"
  [filename container-name tag]
  (build-log/debug-format "Update file `%s`, with tag `%s`" filename tag)
  (if-let [file-content (build-files/read-file filename)]
    (let [searched-pattern (searched-pattern container-name)]
      (if (re-find searched-pattern file-content)
        (spit-workflow* filename file-content searched-pattern tag)
        (do (build-log/warn-format "Not able to update `%s`, the pattern `%s` has not been found"
                                   filename
                                   searched-pattern)
            false)))
    (build-log/warn-format "File %s doesn't exist, workflow update is skipped")))

(defn show-tag-in-workflows
  "Print in log the current workflow tag
  Params:
  * `updates` list of updates, each one is a filename and a container
  * `container-name`"
  [workflows container-name]
  (doseq [filename workflows]
    (if-let [found-tag (slurp-tag filename container-name)]
      (build-log/info-format "Found container `%s` tagged `%s` in file `%s`"
                             container-name
                             found-tag
                             filename)
      (do (build-log/error-format "No tag found in file `%s` for container `%s`"
                                  filename
                                  container-name)
          false))))

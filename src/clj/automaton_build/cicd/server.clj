(ns automaton-build.cicd.server
  "Adapter to the CICD
  Proxy to github

  * When run is github action: that environment variable is set automatically, check [docs](https://docs.github.com/en/actions/learn-github-actions/variables)
  * When run is github action container image, we set manually that variable in the `Dockerfile`(clojure/container-images/gha_runner/Dockerfile)
  * Otherwise, that variable is not set and `is-cicd?` returns false"
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]
   [clojure.string :as str]))

(def ^:private github-env-var "CI")

(defn is-cicd?*
  "Tells if the local instance runs in CICD"
  []
  (boolean (System/getenv github-env-var)))

(def is-cicd? (memoize is-cicd?*))

(defn- update-workflow*
  [filename file-content searched-pattern tag]
  (let [new-content (str/replace file-content searched-pattern (str "$1" tag))]
    (when-not (nil? new-content)
      (build-files/spit-file filename new-content)
      true)))

(defn- searched-pattern
  [container-name]
  (-> (str "(uses:\\s*docker://\\w*/" container-name ")(.*)")
      re-pattern))

(defn- update-workflow
  "Update a workflow file with
  Params:
  * `filename` filename to modify
  * `container-name` name of the container to update
  * `tag` tag to upsert"
  [filename container-name tag]
  (build-log/info-format "Update file `%s`, with tag `%s`" filename tag)
  (if-let [file-content (build-files/read-file filename)]
    (let [searched-pattern (searched-pattern container-name)]
      (if (re-find searched-pattern file-content)
        (update-workflow* filename file-content searched-pattern tag)
        (do (build-log/warn-format
             "Not able to update `%s`, the pattern `%s` has not been found"
             filename
             searched-pattern)
            false)))
    (build-log/warn-format
     "File %s doesn't exist, workflow update is skipped")))

(defn update-workflows
  "Used to update all workflow of a repo to the tag
  Params:
  * `updates` list of updates, each one is a filename and a container
  * `tag` the tag
  * `container-name`"
  [updates tag container-name]
  (->> (for [filename updates] (update-workflow filename container-name tag))
       (every? true?)))

(defn show-tag-in-workflow
  "Show the tag used in workflow file
  Params:
  * `filename` filename to modify
  * `container-name` name of the container to update"
  [filename container-name]
  (if-let [file-content (some-> filename
                                build-files/is-existing-file?
                                build-files/read-file)]
    (let [searched-pattern (searched-pattern container-name)
          search-result (re-find searched-pattern file-content)]
      (if-let [found-tag (last search-result)]
        (build-log/info-format "Found container `%s` tagged `%s` in file `%s`"
                               container-name
                               found-tag
                               filename)
        (do (build-log/error-format
             "No tag found in file `%s` for container `%s`"
             filename
             container-name)
            false)))
    (do (build-log/warn-format
         "File %s doesn't exist, unable to show the container tag in it"
         filename)
        false)))

(defn show-tag-in-workflows
  "Print in log the current tag
  Params:
  * `updates` list of updates, each one is a filename and a container
  * `container-name`"
  [updates container-name]
  (doseq [filename updates] (show-tag-in-workflow filename container-name)))

(comment
  (update-workflows [".github/workflows/commit_validation.yml"]
                    "v0.0.8"
                    "gha-automaton-build:")
  (show-tag-in-workflows [".github/workflows/commit_validation.yml"]
                         "gha-automaton-build:")
  ;
)

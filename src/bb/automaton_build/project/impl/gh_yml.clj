(ns automaton-build.project.impl.gh-yml
  "Github yml workflow file"
  (:require
   [automaton-build.os.filename :as build-filename]
   [clojure.string              :as str]))

(defn workflow-yml
  "File of the workflow named `workflow-name` in the app stored in `app-dir`"
  [app-dir workflow-name]
  (-> app-dir
      (build-filename/create-file-path ".github" "workflows" (str workflow-name ".yml"))))

(defn update-gha-version
  "Update a workflow yml file (described in `filedesc`) with the `version` - concerns only the image `image-name`."
  [filedesc image-name version]
  (some-> (:raw-content filedesc)
          (str/replace (-> (str "(?m)(uses:.*" image-name ":)(.*)$")
                           re-pattern)
                       (str "$1" version))))

(ns automaton-build.cicd.server
  "Adapter to the CICD
  Proxy to github

  * When run is github action: that environment variable is set automatically, check [docs](https://docs.github.com/en/actions/learn-github-actions/variables)
  * When run is github action container image, we set manually that variable in the `Dockerfile`(clojure/container-images/gha_runner/Dockerfile)
  * Otherwise, that variable is not set and `is-cicd?` returns false"
  (:require
   [automaton-build.cicd.deployment.gha-yml :as build-gha-yml]
   [automaton-build.log                     :as build-log]))

(def ^:private github-env-var "CI")

(defn is-cicd?*
  "Tells if the local instance runs in CICD"
  []
  (boolean (System/getenv github-env-var)))

(def is-cicd? (memoize is-cicd?*))

(defn show-tag-in-workflows
  "Print in log the current tag
  Params:
  * `updates` list of updates, each one is a filename and a container
  * `container-name`"
  [workflows container-name]
  (doseq [filename workflows]
    (if-let [found-tag (build-gha-yml/slurp-tag filename container-name)]
      (build-log/info-format "Found container `%s` tagged `%s` in file `%s`"
                             container-name
                             found-tag
                             filename)
      (do (build-log/error-format "No tag found in file `%s` for container `%s`"
                                  filename
                                  container-name)
          false))))

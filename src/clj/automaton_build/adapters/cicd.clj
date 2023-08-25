(ns automaton-build.adapters.cicd
  "Adapter to the CICD
  Proxy to github

  * When run is github action: that environment variable is set automatically, check [docs](https://docs.github.com/en/actions/learn-github-actions/variables)
  * When run is github action container image, we set manually that variable in the `Dockerfile`(clojure/container-images/gha_runner/Dockerfile)
  * Otherwise, that variable is not set and `is-cicd?` returns false")

(def github-env-var
  "CI")

(defn is-cicd?*
  "Tells if the local instance runs in CICD"
  []
  (System/getenv github-env-var))

(def is-cicd?
  (memoize is-cicd?*))

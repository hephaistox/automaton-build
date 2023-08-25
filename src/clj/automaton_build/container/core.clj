(ns automaton-build.container.core
  (:require
   [automaton-build.env-setup :as env-setup]))

(defn container-image-container-names
  "List of container names
  Params:
  * none"
  []
  (->> (:container-repo env-setup/env-setup)
       (keep (fn [[_k v]]
               (when (map? v)
                 (:repo-name v))))
       set))

(defn is-container-image?
  "True if the `image-name` matches an existing container image name
  Params:
  * `image-name` image name to check"
  [image-name]
  (-> (container-image-container-names)
      (contains? image-name)))

(ns automaton-build.monorepo.deps-edn
  (:require
   [automaton-build.os.filename  :as build-filename]
   [automaton-build.project.deps :as build-deps]))

(defn src-paths [paths] (vec (filter #(and (string? %) (re-matches #"^(?!.*test).*" %)) paths)))

(defn generate-paths
  "Generate monorepo deps.edn `:paths` content, based on all `:extra-paths` and `:paths` of apps"
  [target-dir apps]
  (->> apps
       (mapcat (fn [{:keys [app-dir]
                     {deps-edn :edn} :deps}]
                 (let [app-path (build-filename/relativize app-dir target-dir)]
                   (->> (build-deps/extract-paths deps-edn #{})
                        (map #(build-filename/create-dir-path app-path %))))))
       sort
       dedupe
       vec))

(defn- remove-subapps-references
  "Returns `deps` without `sub-apps` libraries"
  [sub-apps deps]
  (->> sub-apps
       (map #(get-in % [:project-config-filedesc :edn :publication :as-lib]))
       (apply dissoc deps)))

(defn generate-deps
  "Return the map containing all dependencies in `apps` deps-edn files.
   Considers both `:deps-edn` and `:extra-deps`"
  [apps]
  (->> apps
       (map (fn [{{app-deps :edn} :deps}]
              (->> app-deps
                   (build-deps/extract-deps #{})
                   (into {}))))
       (apply merge-with build-deps/compare-deps)
       (remove-subapps-references apps)
       (into (sorted-map))))

(defn- test-runner-main-opts
  ([test-paths alias-match]
   (concat ["-m" "cognitect.test-runner"]
           (when alias-match ["-r" alias-match])
           (interleave (repeat "-d") test-paths)))
  ([test-paths] (test-runner-main-opts test-paths nil)))

(defn assoc-test-runner-alias
  [deps-edn {:keys [alias paths match]}]
  (-> deps-edn
      (assoc-in [:aliases alias :main-opts] (test-runner-main-opts paths match))
      (assoc-in [:aliases alias :extra-paths] paths)))

(defn filter-test-paths
  [paths regex]
  (vec (filter #(and (string? %) (re-matches (re-pattern regex) %)) paths)))

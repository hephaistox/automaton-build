(ns automaton-build.app.deps-edn
  "Proxy for `deps.edn` file"
  (:require
   [automaton-build.os.files :as build-files]
   [automaton-build.utils.map :as build-utils-map]
   [automaton-build.os.edn-utils :as build-edn-utils]))

(def deps-edn "deps.edn")

(defn compare-deps
  [deps1 deps2]
  (if (pos? (compare (:mvn/version deps1) (:mvn/version deps2))) deps1 deps2))

(defn get-deps-filename
  "Get the deps-file of the application
  Params:
  * `app-dir` is where the application is stored"
  [app-dir]
  (build-files/create-file-path app-dir deps-edn))

#_{:clj-kondo/ignore [:redefined-var]}
(defn slurp
  "Load the deps.edn file of the app, passed as a parameter,
  Params:
  * `app-dir` the directory of the app, where `deps.edn` is stored
  Returns nil if the file does not exists or is malformed"
  [app-dir]
  (some-> app-dir
          build-files/absolutize
          get-deps-filename
          build-files/is-existing-file?
          build-edn-utils/read-edn))

(defn is-hephaistox-deps
  "For a deps entry, return true if the dependency is from hephaistox monorepo

  Params:
  * `dep` is a pair of value, as seen in the `:deps` map"
  [dep]
  (->> dep
       first
       namespace
       (= "hephaistox")))

(defn hephaistox-deps
  "Filter the
  Params:
  * `deps-edn` the deps-edn file content"
  [deps-edn]
  (->> deps-edn
       :deps
       (filter is-hephaistox-deps)
       keys
       vec))

(defn extract-paths
  "Extracts the `:paths` and `:extra-paths` from a given `deps.edn`
   e.g. {:run {...}}
  Params:
  * `deps-edn` deps.end content
  * `excluded-aliases` (Optional, default #{}) is a collection of aliases to exclude"
  [{:keys [paths aliases]
    :as _deps-edn}
   excluded-aliases]
  (let [selected-aliases (apply dissoc aliases excluded-aliases)
        paths-in-aliases (mapcat (fn [[_alias-name alias-map]]
                                   (->> (select-keys alias-map
                                                     [:extra-paths :paths])
                                        vals
                                        (apply concat)))
                          selected-aliases)]
    (->> paths-in-aliases
         (concat paths)
         sort
         dedupe
         (into []))))

(defn extract-deps
  "Extract dependencies in a `deps.edn` file
  Params:
  * `deps-edn` is the content of the file to search dependencies in
  * `excluded-aliases` is a collection of aliases to exclude"
  [{:keys [deps aliases]
    :as _deps-edn}
   excluded-aliases]
  (let [selected-aliases (apply dissoc aliases excluded-aliases)]
    (->> selected-aliases
         (map (fn [[_ alias-defs]]
                (vals (select-keys alias-defs [:extra-deps :deps]))))
         (apply concat)
         (into {})
         (concat deps)
         (map (fn [[deps-name deps-map]] [deps-name deps-map])))))

(defn replace-deps-aliases
  "Replace deps from deps-edn `alias-map` based on `deps-to-replace` map where key is a dep to replace and value is a new value for that key. If the dep doesn't exist in alias-map, it won't get added."
  [deps-to-replace alias-map]
  (cond-> alias-map
    (contains? alias-map :extra-deps)
    (update :extra-deps (partial build-utils-map/replace-keys deps-to-replace))
    (contains? alias-map :deps)
    (update :deps (partial build-utils-map/replace-keys deps-to-replace))))

#_{:clj-kondo/ignore [:redefined-var]}
(defn spit
  "Spit `content` in the filename path
  Params:
  * `app-dir`
  * `content`"
  ([app-dir content header]
   (build-edn-utils/spit-edn (-> app-dir
                                 build-files/absolutize
                                 get-deps-filename)
                             content
                             header))
  ([app-dir content] (spit app-dir content nil)))

(defn update-test-alias-with-paths
  [test-paths aliases]
  (update-in aliases
             [:env-development-test :main-opts]
             #(concat % (interleave (repeat "-d") test-paths))))

(defn get-bb-deps
  "Returns extra deps from deps-edn map"
  [deps-edn]
  (get-in deps-edn [:aliases :bb-deps :extra-deps]))

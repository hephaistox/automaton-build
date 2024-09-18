(ns automaton-build.project.deps
  "Project `deps.edn` file."
  (:require
   [automaton-build.os.edn-utils-bb :as build-edn]
   [automaton-build.os.filename     :as build-filename]))

(defn deps-edn
  "Read project `deps.edn`."
  [app-dir]
  (-> (build-filename/create-file-path app-dir "deps.edn")
      build-edn/read-edn))

(defn write
  "Spit `content` in the filename path
  Params:
  * `app-dir`
  * `content`"
  ([app-dir content]
   (build-edn/write (-> app-dir
                        (build-filename/create-file-path "deps.edn"))
                    content)))

(defn get-src
  "Returns source directories."
  [deps-edn]
  (->> deps-edn
       :aliases
       vals
       (mapcat :extra-paths)
       (concat (:paths deps-edn))
       (filterv #(re-find #"src|test" %))))

(defn compare-deps
  [deps1 deps2]
  (if (pos? (compare (:mvn/version deps1) (:mvn/version deps2))) deps1 deps2))

(defn extract-deps
  "Extract dependencies in a `deps.edn` file
  Params:
  * `deps-edn` is the content of the file to search dependencies in
  * `excluded-aliases` is a collection of aliases to exclude"
  [excluded-aliases
   {:keys [deps aliases]
    :as _deps-edn}]
  (let [selected-aliases (apply dissoc aliases excluded-aliases)]
    (->> selected-aliases
         (map (fn [[_ alias-defs]] (vals (select-keys alias-defs [:extra-deps :deps]))))
         (apply concat)
         (into {})
         (concat deps)
         (map (fn [[deps-name deps-map]] [deps-name deps-map])))))

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
                                   (->> (select-keys alias-map [:extra-paths :paths])
                                        vals
                                        (apply concat)))
                          selected-aliases)]
    (->> paths-in-aliases
         (concat paths)
         sort
         dedupe
         (into []))))

(defn lib-path
  "Creates a map where key is app library reference and value is it's local directory"
  [base-dir app]
  (let [k (get-in app [:project-config-filedesc :edn :publication :as-lib])
        v {:local/root (build-filename/relativize (:app-dir app)
                                                  (build-filename/absolutize base-dir))}]
    (when k {k v})))

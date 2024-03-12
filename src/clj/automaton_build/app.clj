(ns automaton-build.app
  "The application concept gather all description and setup of the application"
  (:require
   [automaton-build.app-data :as build-app-data]
   [automaton-build.app.build-config :as build-build-config]
   [automaton-build.cicd.version :as build-version]
   [automaton-build.code-helpers.update-deps-clj :as build-update-deps-clj]
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]
   [automaton-build.utils.seq :as build-utils-seq]))

(defn find-apps-paths
  [dir]
  (->> (build-build-config/search-for-build-configs-paths dir)
       (map build-files/extract-path)))

(defn append-app-dir
  "The `paths` in the collection are updated so the `app-dir` is a prefix"
  [app-dir paths]
  (->> paths
       (map (fn [src-item] (build-files/create-dir-path app-dir src-item)))
       sort
       dedupe
       vec))

(defn test-paths
  "Retrive app test paths."
  [{:keys [app-dir]
    :as app}]
  (->> (get-in app [:deps-edn :aliases :common-test :extra-paths])
       (mapv (partial build-files/create-dir-path app-dir))))

(defn lib-path
  "Creates a map where key is app library reference and value is it's local directory"
  [base-dir app]
  (let [k (get-in app [:publication :as-lib])
        v {:local/root (build-files/relativize (:app-dir app)
                                               (build-files/absolutize
                                                base-dir))}]
    (when k {k v})))

(defn get-build-css-filename
  [app css-key]
  (get-in app [:publication :frontend :css css-key]))

(defn get-tailwind-config
  [app]
  (get-in app [:publication :frontend :css :tailwind-config]))

(defn update-app-dep
  "Update specific dependency `dep` with `val` in all deps files in `app-dir`.
   Returns map with directory and result of update (true meaning successful)."
  ([app-dir lib val excluded-dirs]
   (let [dirs-to-update
         (->> app-dir
              build-app-data/project-root-dirs
              (filter #(not (build-utils-seq/contains? excluded-dirs %))))]
     (build-log/trace-format "Update app dep in dir: `%s`" dirs-to-update)
     (into (sorted-map)
           (doseq [dir dirs-to-update]
             [dir (build-update-deps-clj/update-single-dep dir lib val)]))))
  ([app-dir lib val] (update-app-dep app-dir lib val [])))

(defn spit-version-edn
  "Update app version file `version.edn` in `app-dir`. Captures requirement for the version to be consciously decided when saved
  Params:
  * `app-dir` directory of the version to count
  * `app-name`
  * `new-version`"
  ([app-dir app-name new-version]
   (if (build-version/confirm-version? app-name
                                       (build-version/current-version app-dir)
                                       new-version)
     (do (build-version/save-version-file app-dir {:version new-version})
         new-version)
     (do (build-log/warn "Version couldn't be updated without user consent")
         nil))))

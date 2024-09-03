(ns automaton-build.app
  "The application concept gather all description and setup of the application"
  (:require
   [automaton-build.app.build-config :as build-build-config]
   [automaton-build.os.files         :as build-files]))

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
        v {:local/root (build-files/relativize (:app-dir app) (build-files/absolutize base-dir))}]
    (when k {k v})))

(defn get-build-css-filename [app css-key] (get-in app [:publication :frontend :css css-key]))

(defn get-tailwind-config [app] (get-in app [:publication :frontend :css :tailwind-config]))

(ns automaton-build.project.impl.clj-deps
  "Proxy to antq library"
  (:require
   [automaton-build.os.cmds                 :refer [blocking-cmd]]
   [automaton-build.os.edn-utils            :as build-edn]
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.tasks.impl.headers.cmds :as echo-cmds]
   [clojure.set                             :as set]))

(defn clj-outdated-deps
  [app-dir]
  (try
    (let [res (blocking-cmd ["clojure" "-M:antq" "--reporter=edn" "--no-changes"] app-dir)
          deps (build-edn/str->edn (str (:out res)))]
      (if deps
        (-> res
            (assoc :exit 0)
            (assoc :deps deps))
        (assoc
         res
         :msg
         "Antq outdated deps search failed with an error, make sure you have :antq alias in your deps.edn
            :antq {:deps {com.github.liquidz/antq {:mvn/version \"VERSION_HERE\"}}
            :main-opts [\"-m\" \"antq.core\"]}")))
    (catch Exception e {:err e})))

(defn clj-dep->dependency
  [app-dir dep]
  (-> dep
      (select-keys [:file :latest-version :name :version])
      (set/rename-keys {:file :path
                        :version :current-version
                        :latest-version :version})
      (update :path (fn [p] (build-filename/create-file-path app-dir p)))
      (assoc :type :clj-dep)))

(defn update-dep!
  "Update single `dependency`"
  [dependency]
  (-> ["clojure"
       "-M:antq"
       "--upgrade"
       "--force"
       "--no-changes"
       "--focus="
       (str (:name dependency) (when (:version dependency) (str "@" (:version dependency))))]
      (blocking-cmd (:path dependency))))


(defn update-deps!
  "Update all `deps` in `dir`"
  ([dir target-dir deps]
   (let [res (cond-> ["clojure" "-M:antq" "--upgrade" "--no-changes" "--force"]
               (and deps (not-empty deps))
               (concat (mapv #(str "--focus=" (:name %) (when (:version %) (str "@" (:version %))))
                             deps))
               (some? target-dir) (concat ["-d" target-dir])
               true (echo-cmds/blocking-cmd dir "Antq update failed" false))]
     (when-not (= 0 (:exit res))
       {:error (:err res)
        :data res})))
  ([dir deps] (update-deps! dir nil deps)))

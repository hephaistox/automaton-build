(ns automaton-build.code-helpers.update-deps
  (:require
   [automaton-build.app.bb-edn              :as build-bb-edn]
   [automaton-build.app.deps-edn            :as build-deps-edn]
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]
   [automaton-build.code-helpers.antq       :as build-code-helpers-antq]
   [automaton-build.log                     :as build-log]
   [automaton-build.os.commands             :as build-cmds]
   [automaton-build.os.files                :as build-files]
   [automaton-build.os.npm                  :as build-npm]))

(defn add-dependency
  [lib version file-map]
  (assoc file-map
         :dependency
         {:name lib
          :latest-version version}))

(defn add-type [type file-map] (assoc file-map :type type))

(defn add-file-path
  [path file-map]
  (case (:type file-map)
    :deps-edn (assoc file-map :file (build-deps-edn/deps-path path))
    :bb-edn (assoc file-map :file (build-bb-edn/bb-edn-filename-fullpath path))
    :pom-xml (assoc file-map :file (build-pom-xml/pom-xml path))))

(defn update-frontend-deps!
  [app-dir]
  (zero? (ffirst (build-cmds/execute-with-exit-code
                  (build-npm/npm-install-cmd app-dir)
                  (build-npm/npm-update-cmd app-dir)
                  (build-npm/npm-audit-fix-cmd app-dir)))))

(defn update-deps! [deps-maps] (build-code-helpers-antq/update-deps! deps-maps))

(defn exclude-libraries
  "Returns `libs` without those that name is in `excluded-libs-names`"
  [dep-maps excluded-libs-names]
  (reduce (fn [acc dep-map]
            (if (some #(= % (:name (:dependency dep-map))) excluded-libs-names)
              acc
              (conj acc dep-map)))
          []
          dep-maps))

(defn find-outdated-deps-edn
  "Finds outdated deps in deps.edn file"
  [app-dir]
  (->> app-dir
       build-deps-edn/slurp
       build-code-helpers-antq/find-outdated-deps
       (map #(add-dependency (:name %) (:latest-version %) {}))
       (map #(add-type :deps-edn %))
       (map #(add-file-path app-dir %))))

(defn update-app-deps-edn
  "Update all deps.edn dependencies in `app-dir` excluding `exclude-libs`"
  [app-dir exclude-libs]
  (build-log/info "Updating npm libraries...")
  (if (update-frontend-deps! app-dir)
    (do (build-log/info "Updating deps.edn file")
        (-> (find-outdated-deps-edn app-dir)
            (exclude-libraries exclude-libs)
            build-code-helpers-antq/update-deps!)
        true)
    (do (build-log/warn "Npm projects version update failed") false)))

(defn find-app-files
  [app-dir]
  (->> [:deps-edn :bb-edn :pom-xml]
       (map #(add-type % {}))
       (map #(add-file-path app-dir %))
       (filter #(build-files/is-existing-file? (:file %)))))

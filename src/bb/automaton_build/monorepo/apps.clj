(ns automaton-build.monorepo.apps
  "A project map for a monorepo with subprojects.")

(def specify-project ["-p" "--project PROJECT" "Project to build doc for."])

(defn add-monorepo-subprojects
  "Add monorepo subprojects."
  [monorepo-config monorepo-name]
  (let [app-dirs
        (->> (get-in
              monorepo-config
              [:project-config-filedesc :edn :monorepo monorepo-name :apps])
             (mapv :app-dir))]
    (-> monorepo-config
        (assoc :subprojects
               (mapv (fn [app-dir] {:app-dir app-dir}) app-dirs)))))

(defn apply-to-subprojects
  "Apply each function `f` to subprojects."
  [monorepo-config & fs]
  (reduce (fn [monorepo-config f]
            (update monorepo-config :subprojects #(mapv f %)))
          monorepo-config
          fs))

(comment
  (require '[automaton-build.tasks.impl.headers.files :as build-headers-files])
  (-> (build-headers-files/project-config "")
      :edn
      (add-monorepo-subprojects :default))
  ;
)
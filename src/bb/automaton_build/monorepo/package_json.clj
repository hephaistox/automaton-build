(ns automaton-build.monorepo.package-json
  (:require
   [automaton-build.project.package-json :as project-package-json]))

(defn generate-package-json
  [main-package-json package-json-apps]
  (let [dependencies (map #(project-package-json/get-dependencies (get-in % [:package-json :json]))
                          package-json-apps)
        new-package-json (project-package-json/add-dependencies main-package-json dependencies)]
    new-package-json))

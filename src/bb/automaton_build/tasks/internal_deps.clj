(ns automaton-build.tasks.internal-deps
  "Update internal dependencies"
  (:require
   [automaton-build.echo.headers            :refer [clear-prev-line
                                                    errorln
                                                    h1
                                                    h1-error
                                                    h1-error!
                                                    h1-valid
                                                    h1-valid!
                                                    h2
                                                    h2-valid
                                                    normalln]]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.tasks.impl.headers.deps :as headers-deps]))

;; ********************************************************************************
;; Setup
;; ********************************************************************************

(def cli-opts
  (-> [["-p" "--project PROJECT" "Project name to update"]
       ["-l" "--local LOCAL-DIR" "Directory where the project stands"]
       ["-t" "--tag VERSION" "Tag of the version to point"]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def project (get-in cli-opts [:options :project]))
(def tag (get-in cli-opts [:options :tag]))

;; ********************************************************************************
;; Update deps.edn deps
;; ********************************************************************************

(defn- force-dep*
  [app-dir app-name project tag]
  (-> (str "In project `"
           app-name
           "` update all references to project `"
           project
           "` to version `"
           tag
           "`")
      normalln)
  (h1 "Update deps.edn")
  (normalln (-> (headers-deps/deps-edn app-dir)
                (assoc-in [:deps project] tag))))

(comment
  (force-dep* "" "automaton-core" "org.clojars.hephaistox/automaton-build" #:mvn{:version "3.3.0"})
  ;
)

(defn force-dep
  "Force the project to update the dependency named `project` to ``"
  []
  (let [app-name "automaton-core"
        app-dir ""]
    (when-not project (errorln "Project is mandatory"))
    (when-not tag (errorln "Tag is mandatory"))
    (if-not (and project tag)
      (normalln (build-cli-opts/print-usage cli-opts "internal-deps"))
      (force-dep* app-dir app-name project tag))
    true))

;; ********************************************************************************
;; Update bb.edn deps
;; ********************************************************************************

(defn change
  [m app-name path new-val]
  (-> (str "In `" app-name "`, turns `" (get-in m path) "` into `" new-val "`")
      normalln)
  (-> m
      (assoc-in path new-val)))

(defn- force-bb-dep*
  [app-name app-dir project tag]
  (-> (headers-deps/deps-edn app-dir)
      (change app-name [:deps project] tag)
      (headers-deps/save-deps app-dir)))

(comment
  (force-bb-dep* "automaton-core" ""
                 "org.clojars.hephaistox/automaton-build" #:mvn{:version "3.3.0"})
  ;
)

(defn force-bb-deps
  "Force the `bb.edn` dependencies."
  []
  (let [app-name "automaton-core"
        app-dir ""]
    (when-not project (errorln "Project is mandatory"))
    (when-not tag (errorln "Tag is mandatory"))
    (if-not (and project tag)
      (normalln (build-cli-opts/print-usage cli-opts "internal-deps"))
      (force-bb-dep* app-dir app-name project tag))
    true))

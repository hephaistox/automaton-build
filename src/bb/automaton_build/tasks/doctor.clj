(ns automaton-build.tasks.doctor
  "Diagnoses common issue on the monorepo."
  (:require
   [automaton-build.code.formatter           :as build-formatter]
   [automaton-build.data.schema              :as build-data-schema]
   [automaton-build.doc.mermaid-bb           :as build-mermaid-bb]
   [automaton-build.echo.headers             :refer [h1
                                                     h1-error
                                                     h1-valid
                                                     h1-valid!
                                                     h2-error!
                                                     h2-valid!
                                                     normalln
                                                     uri-str]]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.user                  :as build-user]
   [automaton-build.project.config           :as build-project-config]
   [automaton-build.tasks.impl.headers.cmds  :refer [blocking-cmd success]]
   [automaton-build.tasks.impl.headers.files :as    build-headers-files
                                             :refer [print-edn-errors
                                                     print-file-errors]]
   [clojure.string                           :as str]))

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def verbose (get-in cli-opts [:options :verbose]))

(defn- doctor-cmd
  [cmd app-dir valid-msg invalid-msg]
  (let [res (blocking-cmd cmd app-dir invalid-msg verbose)]
    (if (success res)
      {:message valid-msg
       :status :ok}
      {:message invalid-msg
       :details (:out res)})))

(defn project-config
  "Log if the `project.edn` is present readable and a valid edn."
  [app-dir]
  (let [project-desc (build-headers-files/project-config app-dir)
        {:keys [filename edn]} project-desc
        defaulted-edn
        (build-data-schema/add-default-values build-project-config/schema edn)]
    (when-not (print-edn-errors project-desc)
      (if (build-data-schema/valid? build-project-config/schema defaulted-edn)
        {:message (str (uri-str filename)
                       "'s file content is not a valid `edn`.")
         :status :ok}
        {:message (str (uri-str filename) " not conform to `schema`.")
         :details (build-data-schema/humanize build-project-config/schema
                                              edn)}))))

(defn npm-audit
  "Check `npm` is well installed."
  [app-dir]
  (doctor-cmd ["npm" "audit"]
              app-dir
              "npm is valid"
              "npm audit returns errors:"))

(defn user-id
  [app-dir]
  (doctor-cmd (build-user/user-id-cmd)
              app-dir
              "user is found"
              "user is not found"))

(defn group-id
  [app-dir]
  (doctor-cmd (build-user/user-id-cmd)
              app-dir
              "group is found"
              "group is not found"))

(defn mermaid-installed
  "Check mermaid is installed."
  [app-dir]
  (doctor-cmd (build-mermaid-bb/mermaid-pull-cli-cmd)
              app-dir
              "mermaid is valid"
              "mermaid is not properly setup."))

(defn mermaid-running
  "Check mermaid is installed."
  [app-dir]
  (let
    [user-id (-> (build-user/user-id-cmd)
                 (blocking-cmd "" "" false)
                 build-user/id-analyze)
     group-id (-> (build-user/group-id-cmd)
                  (blocking-cmd "" "" false)
                  build-user/id-analyze)
     mermaid-filename
     (->
       (build-file/create-temp-file)
       (build-file/write-file
        "erDiagram\nCUSTOMER ||--o{ ORDER : places\nORDER ||--|{ LINE-ITEM : contains\nCUSTOMER }|..|{ DELIVERY-ADDRESS : uses"))
     tmp-dir (build-file/create-temp-dir)
     {:keys [output-file cmd]} (build-mermaid-bb/mermaid-build-image-cmd
                                mermaid-filename
                                user-id
                                group-id
                                tmp-dir)
     res (blocking-cmd cmd app-dir "Should not appear" false)]
    (if (build-file/is-existing-file? output-file)
      {:message "mmdc is valid"
       :status :ok}
      {:message "mmdc is not properly setup."
       :details (:out res)})))

(defn git-installed
  "Git tooling is installed."
  [app-dir]
  (doctor-cmd ["git" "-v"]
              app-dir
              "Git is properly installed."
              "Don't find remote git repo."))

(defn zprint
  [app-dir]
  (doctor-cmd ["zprint" "-w" "deps.edn"]
              app-dir
              "zprint is has validated `deps.edn`"
              "zprint is not well installed."))

(defn zprint-setup
  [_app-dir]
  (let [home-setup-file-desc (build-formatter/read-home-setup)
        {:keys [raw-content]} home-setup-file-desc]
    (when-not (print-file-errors home-setup-file-desc)
      (if (build-formatter/is-zprint-using-project-setup? raw-content)
        {:message "zprint use properly project setup."
         :status :ok}
        {:message "zprint is using your local configuration."}))))

(defn docker-present
  [_app-dir]
  (doctor-cmd ["docker" "-v"] "." "docker found." "docker not found."))

(defn docker-on
  [_app-dir]
  (doctor-cmd ["docker" "run" "hello-world"]
              "."
              "docker is started."
              "docker is not started."))

(def registry
  [{:check-name "project.edn"
    :fn-to-call project-config}
   {:check-name "docker-present"
    :fn-to-call docker-present}
   {:check-name "docker-on"
    :fn-to-call docker-on}
   {:check-name "group-id"
    :fn-to-call group-id}
   {:check-name "user-id"
    :fn-to-call user-id}
   {:check-name "mermaid installed"
    :fn-to-call mermaid-installed}
   {:check-name "mermaid running"
    :fn-to-call mermaid-running}
   {:check-name "zprint"
    :fn-to-call zprint}
   {:check-name "zprint-setup"
    :fn-to-call zprint-setup}
   {:check-name "git-installed"
    :fn-to-call git-installed}
   {:check-name "npm"
    :fn-to-call npm-audit}])

(defn run
  "Execute all tests."
  []
  (try (normalln "Check monorepo setup.")
       (normalln)
       (let [app-dir ""
             synthesis
             (->> registry
                  (mapv (fn [{:keys [check-name fn-to-call]}]
                          (h1 "Check" check-name)
                          (let [s (new java.io.StringWriter)
                                {:keys [message details status]}
                                (when (fn? fn-to-call)
                                  (binding [*out* s] (fn-to-call app-dir)))]
                            (if (= :ok status)
                              (h1-valid "Check" check-name)
                              (do (h1-error "Failed check" (str check-name ":"))
                                  (normalln message)))
                            (when-not (str/blank? details) (normalln details))
                            (let [s (str s)]
                              (when-not (str/blank? s) (println s)))
                            {check-name (= :ok status)})))
                  (apply merge))]
         (normalln)
         (h1-valid! "Synthesis:")
         (doseq [[check-name check-status] synthesis]
           (if check-status (h2-valid! check-name) (h2-error! check-name))))
       (catch Exception e
         (println "Unexpected error during doctor:")
         (println (pr-str e)))))

(ns automaton-build.wf.doctor
  "Diagnoses common issue on the monorepo."
  (:require
   [automaton-build.code.formatter :as build-code-formatter-bb]
   [automaton-build.data.schema    :as build-data-schema]
   [automaton-build.echo.common    :as build-echo-common]
   [automaton-build.echo.headers   :as build-echo-headers]
   [automaton-build.os.cli-opts    :as build-cli-opts]
   [automaton-build.os.cmds        :as build-commands]
   [automaton-build.os.filename    :as build-filename]
   [automaton-build.project.config :as build-project-config]
   [automaton-build.wf.common      :as build-wf-common]
   [clojure.string                 :as str]))

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(defn- doctor-cmd
  [cmd valid-msg invalid-msg]
  (let [{:keys [exit out]} (build-commands/blocking-cmd cmd)]
    (if (zero? exit)
      {:message valid-msg
       :status :ok}
      {:message invalid-msg
       :details out})))

(defn project-config
  "Log if the `build-data-config.edn` configuration is invalid:
  * so, not matching the schema.
  Returns the error message if any, `nil` otherwise."
  []
  (let [project-config (->> (build-wf-common/project-config)
                            (build-data-schema/add-default-values
                             build-project-config/schema))]
    (if (build-data-schema/valid? build-project-config/schema project-config)
      {:message "`build-data-config.edn` is valid."
       :status :ok}
      {:message (str (build-echo-common/uri (build-filename/absolutize
                                             "build_config.edn"))
                     " not conform to `schema`.")
       :details (build-data-schema/humanize build-project-config/schema
                                            project-config)})))

(defn npm-audit
  "Check `npm` is well installed."
  []
  (doctor-cmd ["npm" "audit"] "npm is valid" "npm audit returns errors:"))

(defn mermaid-toolings
  "Check mermaid tooling presence."
  []
  (doctor-cmd ["mmdc" "help"] "mmdc is valid" "mmdc is not properly setup."))

(defn git-toolings
  "Git tooling presence."
  []
  (let [{:keys [exit out]} (build-commands/blocking-cmd ["git" "remote" "-v"])
        origin-remotes (->> out
                            str/split-lines
                            (map #(str/split % #"\t"))
                            (keep #(= "origin" (first %)))
                            count)]
    (merge {:check-name "git"}
           (cond
             (not (zero? exit)) {:message "git is not properly installed."
                                 :details out}
             (< origin-remotes 2) {:message
                                   "git remote is not properly installed."
                                   :details out}
             :else {:message "git is valid"
                    :status :ok}))))

(defn zprint
  []
  (doctor-cmd ["zprint" "-w" "deps.edn"]
              "zprint is has validated `deps.edn`"
              "zprint is not well installed."))

(defn zprint-setup
  []
  (if (build-code-formatter-bb/is-zprint-using-project-setup?)
    {:message "zprint use properly project setup."
     :status :ok}
    {:message "zprint is using your local configuration."}))

(def registry
  [{:check-name "project.edn"
    :fn-to-call project-config}
   {:check-name "npm"
    :fn-to-call npm-audit}
   {:check-name "mermaid"
    :fn-to-call mermaid-toolings}
   {:check-name "zprint"
    :fn-to-call zprint}
   {:check-name "zprint-setup"
    :fn-to-call zprint-setup}
   {:check-name "git"
    :fn-to-call git-toolings}])

(defn run
  "Execute all tests."
  []
  (build-echo-headers/normalln "Check monorepo setup.")
  (build-echo-headers/normalln)
  (let [synthesis
        (->> registry
             (mapv (fn [{:keys [check-name fn-to-call]}]
                     (build-echo-headers/h1 "Check" check-name)
                     (let [{:keys [message details status]} (fn-to-call)]
                       (if (= :ok status)
                         (build-echo-headers/h1-valid "Check" check-name)
                         (do (build-echo-headers/h1-error "Failed check"
                                                          (str check-name ":"))
                             (build-echo-headers/normalln message)))
                       (when details (build-echo-headers/normalln details))
                       {check-name (= :ok status)})))
             (apply merge))]
    (build-echo-headers/normalln)
    (build-echo-headers/h1 "*")
    (build-echo-headers/h1-valid "Synthesis:")
    (doseq [[check-name check-status] synthesis]
      (build-echo-headers/h2 "*")
      (if check-status
        (build-echo-headers/h2-valid check-name)
        (build-echo-headers/h2-error check-name)))))

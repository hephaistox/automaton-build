(ns automaton-build.wf.2
  "Workflow 2 is starting a local development environment"
  (:require
   [automaton-build.code.frontend-compiler :as build-os-fe-compiler]
   [automaton-build.doc.mermaid-bb         :as build-mermaid-bb]
   [automaton-build.echo.actions           :as build-echo-actions]
   [automaton-build.echo.common            :as build-echo-common]
   [automaton-build.fe.css                 :as build-fe-css]
   [automaton-build.os.cli-opts            :as build-cli-opts]
   [automaton-build.os.file                :as build-file]
   [automaton-build.os.filename            :as build-filename]
   [automaton-build.wf.common              :as build-wf-common]
   [clojure.string                         :as str]))

(defn- inverse
  [cli-opts]
  (update cli-opts
          :options
          #(if (:inverse %)
             (-> %
                 (update :repl not)
                 (update :mermaid not)
                 (update :frontend not)
                 (update :css not))
             %)))

(def ^:private cli-opts
  (->
    [["-r" "--repl" "Don't start the clj REPL" :default true :parse-fn not]
     ["-m" "--mermaid" "Don't watch md files" :default true :parse-fn not]
     ["-f" "--frontend" "Don't start the cljs REPL" :default true :parse-fn not]
     ["-c" "--css" "Don't start the css" :default true :parse-fn not]
     ["-i" "--inverse" "Only set subtasks are executed."]]
    (concat build-cli-opts/help-options
            build-cli-opts/verbose-options
            build-cli-opts/log-options)
    build-cli-opts/parse-cli
    inverse))

(defn- long-living
  [prefixs cmd delay]
  (build-echo-actions/long-living-cmd (get-in cli-opts [:options :verbose])
                                      cmd
                                      prefixs
                                      delay))

(defn- blocking
  [prefixs cmd err-message]
  (build-echo-actions/blocking-cmd (get-in cli-opts [:options :verbose])
                                   cmd
                                   prefixs
                                   err-message))

(defn start-repl
  "Start the REPL."
  [repl-aliases]
  (when (get-in cli-opts [:options :repl])
    (let [prefixs ["repl"]
          action (partial build-echo-actions/action prefixs)
          errorln (partial build-echo-actions/errorln prefixs)]
      (try (if (every? keyword? repl-aliases)
             (let [cmd ["clojure" (apply str "-M" repl-aliases)]]
               (action "Start the clojure REPL.")
               (long-living prefixs cmd 100)
               (errorln "REPL has stopped."))
             (errorln "REPL can't start - aliases are not valid."))
           (catch Exception e
             (errorln "Unexpected error during execution of REPL: " e))))))

(defn css-watch
  []
  (when (get-in cli-opts [:options :css])
    (let [prefixs ["css"]
          errorln (partial build-echo-actions/errorln prefixs)
          actionln (partial build-echo-actions/action prefixs)
          tmp-combined (build-file/create-temp-file "combined.css")]
      (-> tmp-combined
          (build-file/combine-files "resources/css/custom.css"
                                    "resources/css/main.css"))
      (actionln (str "Generate combined css file: "
                     (build-echo-common/uri tmp-combined)))
      (actionln "Watch css modifications.")
      (try (let [cmd (build-fe-css/tailwind-watch-cmd
                      tmp-combined
                      "resources/public/css/compiled/styles.css")]
             (long-living prefixs cmd 100))
           (catch Exception e
             (errorln "Unexpected error during execution of css watch" e))))))

(defn fe-watch
  "Watch the front end."
  [run-aliases]
  (when (get-in cli-opts [:options :frontend])
    (let [prefixs ["fe"]
          errorln (partial build-echo-actions/errorln prefixs)
          actionln (partial build-echo-actions/action prefixs)]
      (actionln "Install frontend deps.")
      (try (blocking prefixs
                     (build-os-fe-compiler/install-cmd)
                     "Install frontend deps has stopped unexpectedly:")
           (if (every? keyword? run-aliases)
             (do (actionln (str "Watch frontend aliases and start REPL: "
                                run-aliases))
                 (let [watch-cmd (-> (mapv name run-aliases)
                                     build-os-fe-compiler/fe-watch-cmd)]
                   (long-living prefixs watch-cmd 100)
                   (errorln "Frontend has stopped unexpectedly")))
             (errorln "REPL can't start - run aliases are not valid"))
           (catch Exception e (errorln "Unexpected error" e))))))

(def generated-image-extension ".png")

(defn- build-new-mermaid-file-list
  "Returns filenames of mermaid files to update, exclude `failed-files`."
  [failed-files normalln]
  (let [mermaid-files (->> (build-mermaid-bb/ls-mermaid build-wf-common/app-dir)
                           (remove failed-files)
                           vec)
        changed-files (-> mermaid-files
                          (build-mermaid-bb/files-to-recompile
                           generated-image-extension)
                          vec)]
    (when (get-in cli-opts [:options :verbose])
      (normalln "Detected mermaid files: " mermaid-files)
      (normalln "Mermaid files to be updated: " changed-files))
    (when-not (empty? failed-files)
      (normalln "Failing files are excluded (unless updated again:)")
      (normalln (str/join " " failed-files)))
    changed-files))

(defn mermaid
  "Watch mermaid files."
  []
  (when (get-in cli-opts [:options :mermaid])
    (try (let [prefixs ["md"]
               actionln (partial build-echo-actions/action prefixs)
               normalln (partial build-echo-actions/normalln prefixs)
               errorln (partial build-echo-actions/errorln prefixs)]
           (actionln (str "Watching mermaid files in sub-directory of "
                          (build-echo-common/uri (build-filename/absolutize
                                                  build-wf-common/app-dir))))
           (loop [failed-files #{}
                  files-to-do #{}]
             (if (empty? files-to-do)
               (do (Thread/sleep 1000)
                   (recur failed-files
                          (build-new-mermaid-file-list failed-files normalln)))
               (let [md-cmd (-> (first files-to-do)
                                (build-mermaid-bb/build-mermaid-image-cmd
                                 generated-image-extension))]
                 (long-living prefixs md-cmd 100)
                 (errorln "Mermaid image generation has failed.")
                 (recur failed-files (rest files-to-do))))))
         (catch Exception e (println "error" e)))))

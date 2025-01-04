(ns automaton-build.tasks.2
  "Workflow 2 is starting a local development environment"
  (:require
   [automaton-build.code.cljs               :as build-cljs]
   [automaton-build.echo.actions            :refer [action errorln exceptionln normalln uri-str]]
   [automaton-build.fe.css                  :as build-fe-css]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.tasks.impl.actions.cmds :refer [blocking-cmd long-living-cmd]]
   [clojure.string                          :as str]))

(def ^:private cli-opts
  (-> [["-r" "--repl" "Don't start the clj REPL" :default true :parse-fn not]
       ["-m" "--mermaid" "Don't watch md files" :default true :parse-fn not]
       ["-M" "--mermaid-all" "Force generation of all files again"]
       ["-f" "--frontend" "Don't start the cljs REPL" :default true :parse-fn not]
       ["-c" "--css" "Don't start the css" :default true :parse-fn not]]
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              build-cli-opts/inverse-options)
      build-cli-opts/parse-cli
      (build-cli-opts/inverse [:repl :mermaid :frontend :css])))

(def verbose (get-in cli-opts [:options :verbose]))

(defn start-repl
  "Start the REPL."
  [repl-aliases]
  (let [prefixs ["repl"]
        app-dir ""
        action (partial action prefixs)
        errorln (partial errorln prefixs)
        exceptionln (partial exceptionln prefixs)
        long-living-cmd (partial long-living-cmd prefixs)]
    (try (when (get-in cli-opts [:options :repl])
           (if (every? keyword? repl-aliases)
             (let [cmd ["clojure" (apply str "-M" repl-aliases)]]
               (action "Start the clojure REPL.")
               (long-living-cmd cmd app-dir 100 verbose (constantly true) (constantly true))
               (errorln "REPL has stopped."))
             (errorln "REPL can't start - aliases are not valid.")))
         (catch Exception e
           (if (str/includes? (pr-str e) "Address already in use")
             (do (errorln "A REPL is still running.")
                 (normalln "Execute" "prgrep java" "to know what process to kill."))
             (do (errorln "Unexpected error during execution of REPL: ") (exceptionln e)))))))

(defn css-watch
  "Watch the css modificatoin with tailwind."
  []
  (try (when (get-in cli-opts [:options :css])
         (let [prefixs ["css"]
               app-dir ""
               tmp-combined (build-file/create-temp-file "combined.css")
               action (partial action prefixs)
               long-living-cmd (partial long-living-cmd prefixs)]
           (action "Generate combined css file:" (uri-str tmp-combined))
           (-> tmp-combined
               (build-file/combine-files "resources/css/custom.css" "resources/css/main.css"))
           (action "Watch css modifications.")
           (let [cmd (-> tmp-combined
                         (build-fe-css/tailwind-watch-cmd
                          "resources/public/css/compiled/styles.css"))]
             (long-living-cmd cmd app-dir 100 verbose (constantly true) (constantly true)))))
       (catch Exception e
         (println "Unexpected error during execution of css watch")
         (println (pr-str e)))))

(defn fe-watch
  "Watch the front end."
  [run-aliases]
  (try (when (get-in cli-opts [:options :frontend])
         (let [prefixs ["fe"]
               app-dir ""
               errorln (partial errorln prefixs)
               action (partial action prefixs)
               blocking-cmd (partial blocking-cmd prefixs)
               long-living-cmd (partial long-living-cmd prefixs)]
           (action "Install frontend deps.")
           (blocking-cmd (build-cljs/install-cmd)
                         app-dir
                         "Install frontend deps has stopped unexpectedly:"
                         verbose)
           (if (every? keyword? run-aliases)
             (do (action "Watch frontend aliases and start REPL:" run-aliases)
                 (-> (mapv name run-aliases)
                     build-cljs/cljs-watch-cmd
                     (long-living-cmd app-dir 100 verbose (constantly true) (constantly true))))
             (errorln "REPL can't start - run aliases are not valid"))))
       (catch Exception e (errorln "Unexpected error" e))))

(def generated-image-extension ".png")

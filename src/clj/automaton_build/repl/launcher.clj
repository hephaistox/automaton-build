(ns automaton-build.repl.launcher
  "This namespace is apart from repl to allow initialization of log before the init of configuration for instance"
  (:require
   [automaton-build.configuration :as build-conf]
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]
   [automaton-build.os.terminal-msg :as build-terminal-msg]
   [automaton-build.repl.portal :as build-portal]
   [cider.nrepl :as cider-nrepl]
   [nrepl.server :refer [default-handler start-server stop-server]]
   [refactor-nrepl.middleware]))

(defonce nrepl-port-filename ".nrepl-port")

(defn custom-nrepl-handler
  "We build our own custom nrepl handler"
  [nrepl-mws]
  (apply default-handler nrepl-mws))

(def repl "Store the repl instance in the atom" (atom {}))

(defn get-nrepl-port-parameter
  []
  (build-conf/read-param [:dev :clj-nrepl-port] 8000))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn get-active-nrepl-port
  "Retrieve the nrepl port, available for REPL"
  []
  (:nrepl-port @repl))

(defn stop-repl
  "Stop the repl"
  [repl-port]
  (build-log/info "Stop nrepl server on port" repl-port)
  (stop-server (:repl @repl))
  (reset! repl {}))

(defn create-nrepl-files
  "Consider all deps.edn files as the root of a clojure project and creates a .nrepl-port file next to it"
  [repl-port]
  (let [build-configs (build-files/search-files "" "**build_config.edn")
        nrepl-ports (map #(build-files/file-in-same-dir % nrepl-port-filename)
                         build-configs)]
    (doseq [nrepl-port nrepl-ports]
      (build-files/spit-file (str nrepl-port) repl-port))))

(defn start-repl*
  "Launch a new repl

   In debug mode, show all the details for build log"
  [middleware]
  (let [repl-port (get-nrepl-port-parameter)]
    (create-nrepl-files repl-port)
    (reset! repl {:nrepl-port repl-port
                  :repl
                  (do
                    (build-log/info "nrepl available on port " repl-port)
                    (build-terminal-msg/println-msg "repl port is available on:"
                                                    repl-port)
                    (start-server :port repl-port
                                  :handler (custom-nrepl-handler middleware)))})
    (build-portal/start)
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(do (build-log/info "SHUTDOWN in progress, stop repl on port `"
                                   repl-port
                                   "`")
                   (-> (build-files/search-files ""
                                                 (str "**" nrepl-port-filename))
                       (build-files/delete-files))
                   (stop-repl repl-port)
                   (build-portal/stop))))))

(defn start-repl
  "Start repl, setup and catch errors"
  []
  (try (start-repl* (conj cider-nrepl/cider-middleware
                          'refactor-nrepl.middleware/wrap-refactor))
       :started
       (catch Exception e
         (build-log/error (ex-info "Uncaught exception" {:error e})))))

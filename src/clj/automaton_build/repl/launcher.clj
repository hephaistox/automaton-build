(ns automaton-build.repl.launcher
  "This namespace is apart from repl to allow initialization of log before the init of configuration for instance"
  (:require
   [automaton-build.configuration :as build-conf]
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]
   [automaton-build.repl.portal.server :as build-repl-portal]
   [automaton-build.os.terminal-msg :as build-terminal-msg]
   [nrepl.server :refer [default-handler start-server stop-server]]))

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

(defn- stop-repl
  "Stop the repl"
  []
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
    (build-repl-portal/start)
    (reset! repl {:nrepl-port repl-port
                  :repl
                  (do
                    (build-log/info "nrepl available on port " repl-port)
                    (build-terminal-msg/println-msg "repl port is available on:"
                                                    repl-port)
                    (start-server :port repl-port
                                  :handler (custom-nrepl-handler middleware)))})
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(do (build-log/info "SHUTDOWN in progress, stop repl on port `"
                                   repl-port
                                   "`")
                   (build-repl-portal/stop)
                   (-> (build-files/search-files ""
                                                 (str "**" nrepl-port-filename))
                       (build-files/delete-files))
                   (stop-repl))))))
(defn- require-package
  [package]
  (-> package
      namespace
      symbol
      require))

(defn- require-existing-package
  [package]
  (try (require-package package) package (catch Exception _ nil)))

(defn add-packages
  [packages]
  (reduce (fn [acc package]
            (if-let [confirmed-package (require-existing-package package)]
              (if (coll? @(resolve confirmed-package))
                (vec (concat @(resolve confirmed-package) acc))
                (conj acc confirmed-package))
              acc))
          []
          packages))

(defn default-middleware
  []
  (add-packages ['cider.nrepl/cider-middleware
                 'refactor-nrepl.middleware/wrap-refactor]))

(defn start-repl
  "Start repl, setup and catch errors"
  [mdws]
  (try (start-repl* mdws)
       :started
       (catch Exception e
         (build-log/error (ex-info "Uncaught exception" {:error e})))))

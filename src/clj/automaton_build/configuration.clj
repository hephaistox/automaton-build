(ns automaton-build.configuration
  "Configuration parameters, stored in configuration file.
   This namespace is the entry point to call conf "
  (:require
   [automaton-build.configuration.environment :as build-conf-env]
   [automaton-build.configuration.files :as build-conf-files]
   [automaton-build.configuration.protocol :as build-conf-prot]
   [automaton-build.log :as build-log]))

(defn start-conf
  []
  (try (build-log/debug "Starting configuration")
       (let [files (build-conf-files/make-files-conf)
             env (build-conf-env/make-env-conf)]
         [files env])
       (catch Throwable e
         (build-log/fatal-exception
          (ex-info "Configuration failed, application will stop" {:error e}))
         (throw e))))

(defn stop-conf [] (build-log/debug "Stop configuration"))

(def conf-state (start-conf))

(defn read-param
  ([key-path default-value]
   (if (not (vector? key-path))
     (do
       (build-log/warn-format
        "Key path should be a vector. I found `%s`, default value `%s` is returned"
        key-path
        default-value)
       default-value)
     (let [value
           (or (build-conf-prot/read-conf-param (first conf-state) key-path)
               (build-conf-prot/read-conf-param (second conf-state) key-path))]
       (if (nil? value)
         (do (build-log/trace-format
              "Read key-path %s returned nil, defaulted to `%s`"
              key-path
              default-value)
             default-value)
         (do (build-log/trace "Read key-path " key-path " = " value) value)))))
  ([key-path] (read-param key-path nil)))

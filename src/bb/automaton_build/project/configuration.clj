(ns automaton-build.project.configuration
  "Configuration parameters, stored in configuration file.
   This namespace is the entry point to call conf"
  (:require
   [automaton-build.echo.headers              :refer [h1-error! normalln]]
   [automaton-build.project.impl.config-env   :as build-conf-env]
   [automaton-build.project.impl.config-files :as build-conf-files]
   [automaton-build.project.impl.config-prot  :as build-conf-prot]))

(defn start-conf
  []
  (try
    (normalln "Starting configuration")
    (let [files (build-conf-files/make-files-conf) env (build-conf-env/make-env-conf)] [files env])
    (catch Throwable e (h1-error! "Configuration failed, application will stop" {:exception e}))))

(defn stop-conf [] (normalln "Stop configuration"))

(def conf-state (memoize start-conf))

(defn read-param
  ([key-path default-value]
   (if (not (vector? key-path))
     (do (normalln (format
                    "Key path should be a vector. I found `%s`, default value `%s` is returned"
                    key-path
                    default-value))
         default-value)
     (let [value (or (build-conf-prot/read-conf-param (first (conf-state)) key-path)
                     (build-conf-prot/read-conf-param (second (conf-state)) key-path))]
       (if (nil? value) default-value value))))
  ([key-path] (read-param key-path nil)))

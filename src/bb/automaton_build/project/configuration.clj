(ns automaton-build.project.configuration
  "Configuration parameters, stored in configuration file.
   This namespace is the entry point to call conf"
  (:require
   [automaton-build.project.impl.config-env   :as build-conf-env]
   [automaton-build.project.impl.config-files :as build-conf-files]
   [automaton-build.project.impl.config-prot  :as build-conf-prot]))

(defn start-conf
  []
  (try
    (let [files (build-conf-files/make-files-conf) env (build-conf-env/make-env-conf)] [files env])
    (catch Throwable e {:exception e})))

(def conf-state (memoize start-conf))

(defn read-param
  ([key-path default-value]
   (if (not (vector? key-path))
     default-value
     (let [value (or (build-conf-prot/read-conf-param (first (conf-state)) key-path)
                     (build-conf-prot/read-conf-param (second (conf-state)) key-path))]
       (if (nil? value) default-value value))))
  ([key-path] (read-param key-path nil)))

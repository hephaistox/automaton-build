(ns automaton-build.project.impl.config-files
  "Simple configuration based on files
  Data can be set in java property `heph-conf`, if more files are needed you can pass them separated by `,`"
  (:require
   [automaton-build.project.impl.config-prot :as build-conf-prot]
   [babashka.fs                              :as fs]
   [clojure.edn                              :as edn]
   [clojure.string                           :as str]))

(defn read-edn
  "Simple read.edn function, defined here to limit requires for configuration
  Read the `.edn` file,
  Params:
  * `edn-filename` name of the edn file to load
  Errors:
  * throws an exception if the file is not found
  * throws an exception if the file is a valid edn
  * `file` could be a string representing the name of the file to load
  or a (io/resource) object representing the name of the file to load"
  [edn-filename]
  (let [edn-filename (when edn-filename (str (fs/absolutize edn-filename)))
        edn-content (try (slurp edn-filename) (catch Exception _ nil))]
    (when edn-content (try (edn/read-string edn-content) (catch Exception _ nil)))))

(defrecord FilesConf [config-map]
  build-conf-prot/Conf
    (read-conf-param [_this key-path] (get-in config-map key-path)))

(def ^:private default-config-files ["env/development/config.edn" "env/common_config.edn"])

(defn- property->config-files
  "Turn java property into sequence of config file paths"
  [property-name]
  (some-> property-name
          System/getProperty
          (str/split #",")))

(defn ensure-config-files
  "This is done in case the project does not have access to jvm-opts. E.g. when tasks are from bb.edn"
  [config-files]
  (if-not (and (nil? config-files) (empty? config-files)) config-files default-config-files))

(defn make-files-conf
  "Create the simple configuration"
  []
  (let [config-map (->> "heph-conf"
                        property->config-files
                        ensure-config-files
                        (mapv read-edn)
                        (filterv some?)
                        (apply merge))]
    (->FilesConf config-map)))

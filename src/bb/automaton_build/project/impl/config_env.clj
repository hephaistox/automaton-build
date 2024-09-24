(ns automaton-build.project.impl.config-env
  (:require
   [automaton-build.data.keyword             :as build-data-keyword]
   [automaton-build.project.impl.config-prot :as build-conf-prot]
   [clojure.string                           :as str]))

(defn env-key-path
  "Turns key-path into environment type key."
  [key-path]
  (let [path-str (str/join "-" (map name key-path))]
    (when-not (str/blank? path-str) (keyword path-str))))

(defrecord EnvConf [config-map]
  build-conf-prot/Conf
    (read-conf-param [_this key-path] (get config-map (env-key-path key-path))))

(defn read-system-env
  "Reads system env properties and converts to appropriate type."
  []
  (->> (System/getenv)
       (map (fn [[k v]] [(build-data-keyword/keywordize k) v]))
       (into {})))

(defn make-env-conf "Create the simple configuration" [] (->EnvConf (read-system-env)))

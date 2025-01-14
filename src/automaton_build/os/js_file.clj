(ns automaton-build.os.js-file
  (:require
   [automaton-build.os.file :as build-file]
   [clojure.string          :as str]))

(defn join-config-items
  "Joins config items (like presets requires, content paths etc.). Any items in a way that is acceptable by js config files"
  [config-items]
  (str/join "," config-items))

(defn js-require
  "Turns `package-name` into js require"
  [package-name]
  (str "require('" package-name "')"))

(defn load-js-config [filepath] (build-file/read-file filepath))

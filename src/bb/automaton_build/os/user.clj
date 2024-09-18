(ns automaton-build.os.user
  "Return system wide user informations."
  (:require
   [clojure.string :as str]))

(defn group-id-cmd ([user] ["id" "-g" user]) ([] ["id" "-g"]))

(defn id-analyze
  [res]
  (-> res
      :out
      str/split-lines
      first
      str/trim))

(defn user-id-cmd ([user] ["id" "-u" user]) ([] ["id" "-u"]))


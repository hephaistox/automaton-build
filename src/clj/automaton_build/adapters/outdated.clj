(ns automaton-build.adapters.outdated
  "Proxy to antq lib.
  This proxy is limited to the build as it is not meant to ends up in production"
  (:require
   [antq.core :as antq]))

(defn upgrade
  "Upgrade the list of apps named
  Params:
  * `app-names` list of application names"
  [app-names]
  (apply antq/-main "--upgrade"
         (mapcat (fn [app-name]
                   (vector "-d" app-name))
                 app-names)))

#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-build.repl.portal.server
  "Portal server starting"
  (:require
   [automaton-build.configuration :as build-conf]
   [portal.api :as p]))

(def ^:private submit "Sumbitting data to the portal" #'p/submit)
(defn- portal-connect "Regular portal add-tap fn proxy." [] (add-tap #'submit))

(defn start
  "Starts portal app
   Params:
   * port (optional) defaults to `default-port`, defines what port portal should be started."
  ([] (start (build-conf/read-param [:dev :portal-port] 8351)))
  ([port]
   (p/open {:port port})
   (portal-connect)
   (tap> (format "Portal server has started for app `%s` on port %d"
                 (build-conf/read-param [:app-name])
                 port))))

(defn stop "Close portal app" [] (p/close))

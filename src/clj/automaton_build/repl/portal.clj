#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-build.repl.portal
  (:require
   [automaton-build.configuration :as build-conf]
   [portal.api :as p]
   [portal.client.jvm :as p-client]))

(defn- default-port [] (build-conf/read-param [:dev :portal-port]))

(def submit #'p/submit)

(defn client-connect
  "Connects to existing portal (start fn).
   Params:
   * `port` (optional)  defaults to def `default-port`, it is a port on which portal app can be found."
  ([] (client-connect (default-port)))
  ([port]
   (build-conf/read-param [:app-name])
   #_{:clj-kondo/ignore [:inline-def]}
   (def client-submit (partial p-client/submit {:port port}))
   (add-tap #'client-submit)
   (tap> "Client connected")))

(defn portal-connect "Regular portal add-tap fn proxy." [] (add-tap #'submit))

(defn start
  "Starts portal app
   Params:
   * port (optional) defaults to `default-port`, defines what port portal should be started."
  ([] (start (default-port)))
  ([port] (p/open {:port port}) (portal-connect) (tap> "Portal has started")))

(defn stop "Close portal app" [] (p/close))

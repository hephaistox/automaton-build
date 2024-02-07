(ns automaton-build.configuration.protocol)

(defprotocol Conf
  (read-conf-param [this key-path]
   "Read the value of key"))

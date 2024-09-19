(ns automaton-build.project.impl.config-prot)

(defprotocol Conf
  (read-conf-param [this key-path]
   "Read the value of key"))

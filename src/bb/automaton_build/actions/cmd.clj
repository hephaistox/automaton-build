(ns automaton-build.actions.cmd
  "Execute command and prints as an action"
  (:refer-clojure :exclude [delay])
  (:require
   [automaton-build.echo.actions :refer [errorln normalln]]
   [automaton-build.os.cmd       :as build-cmd]))


(defn to-str [cmd] (build-cmd/to-str cmd))

;; ********************************************************************************
;; Private
;; ********************************************************************************
(defn- on-out [& s] (apply normalln s))
(defn- on-err [& s] (apply errorln s))
(defn- on-end [] (normalln "finished"))
(defn- cant-start [] (normalln "Impossible to start this command"))

;; ********************************************************************************
;; Low level API
;; ********************************************************************************

(defn create-process
  [cmd dir delay max-out-lines max-err-lines]
  (build-cmd/create-process cmd
                            dir
                            on-out
                            on-err
                            on-end
                            delay
                            cant-start
                            max-out-lines
                            max-err-lines))

(defn still-running? [process] (build-cmd/still-running? process))

(defn wait-for [process] (build-cmd/wait-for process on-out on-err))

(defn kill [process] (build-cmd/kill process on-out))

;; ********************************************************************************
;; High level API
;; ********************************************************************************
(defn muted [cmd dir] (build-cmd/muted cmd dir))

(defn muted-non-blocking [cmd dir] (build-cmd/muted-non-blocking cmd dir))

(defn as-string [cmd dir] (build-cmd/as-string cmd dir))

(defn printing-non-blocking
  [cmd dir delay]
  (build-cmd/printing-non-blocking cmd dir on-out on-err on-end delay))

(defn printing [cmd dir delay] (build-cmd/printing cmd dir on-out on-err on-end delay))

(defn print-on-error
  [cmd dir delay max-out-lines max-err-lines]
  (build-cmd/print-on-error cmd dir on-out on-err delay max-out-lines max-err-lines))

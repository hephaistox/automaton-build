(ns automaton-build.echo
  (:require
   [automaton-build.echo.actions           :as build-echo-actions]
   [automaton-build.echo.headers           :as build-echo-headers]
   [automaton-build.echo.one-liner-headers :as build-echo-one-liner-headers]))

(def printers
  {:headers build-echo-headers/printers
   :one-liner-headers build-echo-one-liner-headers/printers
   :action-printers build-echo-actions/printers})

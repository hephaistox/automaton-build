(ns automaton-build.echo.one-liner-headers
  "Print text with headers on the terminal.

  They are one liner, meaning that "
  (:require
   [automaton-build.echo.base    :as build-echo-base]
   [automaton-build.echo.headers :as build-echo-headers]
   [automaton-build.os.text      :as build-text]))

(defn h1-error!
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 1)
  (print build-text/font-red)
  (build-echo-headers/header-printing "!" texts)
  (print build-text/font-default))

(defn h1-valid!
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 1)
  (print build-text/font-green)
  (build-echo-headers/header-printing ">" texts)
  (print build-text/font-default))

(defn h2-error!
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 2)
  (print build-text/font-red)
  (build-echo-headers/header-printing "!" texts)
  (print build-text/font-default))

(defn h2-valid!
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 2)
  (print build-text/font-green)
  (build-echo-headers/header-printing ">" texts)
  (print build-text/font-default))

(defn h3-valid!
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 3)
  (print build-text/font-green)
  (build-echo-headers/header-printing ">" texts)
  (print build-text/font-default))

(defn h3-error!
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 3)
  (print build-text/font-red)
  (build-echo-headers/header-printing "!" texts)
  (print build-text/font-default))

(def printers
  "Printers specific for one line header"
  (merge build-echo-headers/overriden-printers
         {:h1-valid! h1-valid!
          :h2-valid! h2-valid!
          :h3-valid! h3-valid!
          :h1-error! h1-error!
          :h2-error! h2-error!
          :h3-error! h3-error!}))

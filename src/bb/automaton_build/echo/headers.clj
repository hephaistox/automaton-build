(ns automaton-build.echo.headers
  "Prints text with headers on the terminal.

  Each content is modified to wrap the `width` of the terminal, and is modified to have a left margin."
  (:require
   [automaton-build.echo.base :as build-echo-base]
   [automaton-build.os.text   :as build-text]))

;; ********************************************************************************
;; Private
;; ********************************************************************************
(defn- current-left-margin
  "Returns the string of the margin to add."
  []
  (->> (-> @build-echo-base/echo-param
           (get :section 0)
           (repeat \space))
       (apply str)))

;; Screenify
(defn- screenify
  "Prepare the `texts` strings to be printed on the screen."
  [texts]
  (let [prefix-str (current-left-margin)]
    (-> (build-echo-base/screenify prefix-str texts)
        (assoc :prefix-str prefix-str))))

;; Standardized echoing functions
(defn- pure-printing
  [texts]
  (let [{:keys [prefix-str wrapped-strs]} (screenify texts)]
    (doseq [wrapped-str wrapped-strs] (println (str prefix-str wrapped-str)))))

(defn header-printing
  "Print `texts` with the header prefix called `prefix`."
  [prefix texts]
  (let [{:keys [prefix-str wrapped-strs]} (screenify texts)]
    (doseq [wrapped-str wrapped-strs]
      (println (str (apply str (butlast prefix-str)) prefix wrapped-str)))))

;; ********************************************************************************
;; Overridden echoing functions
;; ********************************************************************************
(defn normalln
  "Print text without decoration, with a carriage return included."
  [& texts]
  (print build-echo-base/normal-font-color)
  (pure-printing texts)
  (print build-text/font-default))

(defn errorln
  "To print some text that is an error, should be highlighted and rare."
  [& texts]
  (print build-echo-base/error-font-color)
  (pure-printing texts)
  (print build-text/font-default))

(defn exceptionln "Display exception `e`." [e] (errorln (ex-cause e)) (normalln (pr-str e)))

(defn print-exec-cmd
  "Prints the execution of command string `cmd` with the `prefixs` added."
  [cmd]
  (normalln (build-echo-base/exec-cmd cmd)))

;; ********************************************************************************
;; Headers
;; ********************************************************************************
(defn h1
  [& texts]
  (print build-text/font-white)
  (swap! build-echo-base/echo-param assoc :section 1)
  (header-printing "*" texts)
  (print build-text/font-default))

(defn h1-error
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 1)
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-red))
  (header-printing "!" texts)
  (print build-text/font-default)
  (print build-text/font-default))

(defn h1-valid
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 1)
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-green))
  (header-printing ">" texts))

(defn h2
  [& texts]
  (print build-text/font-white)
  (swap! build-echo-base/echo-param assoc :section 2)
  (header-printing "*" texts)
  (print build-text/font-default))

(defn h2-error
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 2)
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-red))
  (header-printing "!" texts)
  (print build-text/font-default))

(defn h2-valid
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 2)
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-green))
  (header-printing ">" texts)
  (print build-text/font-default))

(defn h3
  [& texts]
  (print build-text/font-white)
  (swap! build-echo-base/echo-param assoc :section 3)
  (header-printing "*" texts)
  (print build-text/font-default))

(defn h3-error
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 3)
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-red))
  (header-printing "!" texts)
  (print build-text/font-default))

(defn h3-valid
  [& texts]
  (swap! build-echo-base/echo-param assoc :section 3)
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-green))
  (header-printing ">" texts)
  (print build-text/font-default))

(comment
  (h2 "test  very long one......")
  (errorln "zeaa")
  (h1-error " failed testt")
  (h1-valid " valid test"))

(def overriden-printers
  "Generic printers overriden for headers"
  {:normalln normalln
   :errorln errorln
   :exceptionln exceptionln
   :print-exec-cmd print-exec-cmd
   :h1 h1
   :h2 h2
   :h3 h3})

(def printers
  "Printers for headers"
  (merge overriden-printers
         {:h1-valid h1-valid
          :h2-valid h2-valid
          :h3-valid h3-valid
          :h1-error h1-error
          :h2-error h2-error
          :h3-error h3-error}))

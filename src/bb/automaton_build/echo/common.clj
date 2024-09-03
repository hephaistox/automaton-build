(ns automaton-build.echo.common
  "Common functions for echoing. Don't use this namespace but one of the implementation of echoing, like headers or actions."
  (:require
   [automaton-build.os.text :as build-text]
   [clojure.pprint          :as pp]
   [clojure.string          :as str]))

(def echo-param "Global echo configuration." (atom {:width 240}))

;; Parameters for echoing implementations

(def normal-font-color build-text/font-default)
(def error-font-color build-text/font-red)

;; Screenify

(defn- wrap-str
  "Splits `str` in as many lines than needed to not exceed the `size`."
  [size str]
  (if (pos? size)
    (let [line-feed-back "…"
          cline-feed-back (count line-feed-back)
          remaining-size (- size cline-feed-back)]
      (loop [str str
             wrapped []]
        (if (<= (count str) size)
          (conj wrapped str)
          (recur (subs str remaining-size)
                 (conj wrapped (format "%s%s" (subs str 0 remaining-size) line-feed-back))))))
    (do (println "Unexpected print error, size " size) (println (pr-str str)))))

(defn screenify
  "Prepare the `texts` strings to be printed on the screen.
  The `prefix` is added to every line."
  [prefix texts]
  (let [wrapped-width (- (:width @echo-param) (count prefix))]
    {:wrapped-width wrapped-width
     :wrapped-strs (->> (str/join " " texts)
                        str/split-lines
                        (mapcat #(wrap-str wrapped-width %))
                        (map #(str prefix %))
                        vec)}))

;; Standardized formatting

(defn cmd-str
  "Wrap a command string `cmd-str` before printing to be seen as a uri in the terminal."
  [cmd-str]
  (str "`" cmd-str "`"))

(defn exec-cmd-str
  "Returns the string of the execution of a command `cmd-str`"
  [cmd-str]
  (str "exec on bash: " (automaton-build.echo.common/cmd-str cmd-str)))

(defn pprint-str "Pretty print `data`" [data] (with-out-str (pp/pprint data)))

(defn uri-str "Returns the string of the `uri`." [uri] (str "`" uri "`"))

(defn current-time-str
  "Returns current time string."
  []
  (.format (java.text.SimpleDateFormat. "HH:mm:ss:SSS") (java.util.Date.)))

(defn build-writter
  "Creates a writter that could be binded to the output with `(binding [*out* s])` where `s` is the result of this function."
  []
  (new java.io.StringWriter))

(defn print-writter
  "Print `str-writter` if necessary."
  [str-writter]
  (let [str-writter (str str-writter)] (when-not (str/blank? str-writter) (print str-writter))))

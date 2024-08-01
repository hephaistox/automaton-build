(ns automaton-build.echo.common
  "Common functions for echoing"
  (:require
   [automaton-build.os.text :as build-text]
   [clojure.pprint          :as pp]
   [clojure.string          :as str]))

(def echo-param "Global echo configuration." (atom {:width 240}))

(comment
  (reset! echo-param {:width 3}))

(defn current-time-str
  "Returns current time string."
  []
  (.format (java.text.SimpleDateFormat. "HH:mm:ss:SSS") (java.util.Date.)))

(defn wrap-str
  "Splits `str` in as many lines than needed to not exceed the `size`."
  [size str]
  (let [line-feed-back "…"
        cline-feed-back (count line-feed-back)
        remaining-size (- size cline-feed-back)]
    (loop [str str
           wrapped []]
      (if (<= (count str) size)
        (conj wrapped str)
        (recur (subs str remaining-size)
               (conj
                wrapped
                (format "%s%s" (subs str 0 remaining-size) line-feed-back)))))))

(defn screenify
  "Prepare the `texts` strings to be printed on the screen."
  [prefix texts]
  (let [wrapped-width (- (:width @echo-param) (count prefix))
        strs (->> (str/join " " texts)
                  str/split-lines)]
    {:wrapped-width wrapped-width
     :wrapped-strs (->> strs
                        (mapcat #(wrap-str wrapped-width %))
                        (map #(str prefix %))
                        vec)}))

(def reset-color build-text/font-default)

(defn pure-printing
  "Use `normalln` instead of this one as a user, except if you're building your own echoing system."
  [prefix texts]
  (let [{:keys [wrapped-strs]} (screenify prefix texts)]
    (doseq [l wrapped-strs] (println l)))
  (print reset-color))

(def normal-font-color build-text/font-default)

(defn normalln
  "Print text without decoration, with a carriage return included."
  [prefix & texts]
  (print normal-font-color)
  (pure-printing prefix texts))

(def error-font-color build-text/font-red)

(defn errorln
  "To print some text that is an error, should be highlighted and rare."
  [prefix & texts]
  (print error-font-color)
  (pure-printing prefix texts))

(defn exceptionln [e] (apply println (ex-cause e)))

(defn wrap-command
  "Wrap a command string `cmd-str` before printing to be seen as a uri in the terminal."
  [cmd-str]
  (str "`" cmd-str "`"))

(defn print-cmd-str
  [prefix cmd-str]
  (normalln prefix (str "exec on bash:" (wrap-command cmd-str))))

(defn print-cmd [prefix cmd] (print-cmd-str prefix (str/join " " cmd)))

(defn pprint "Pretty print `data`" [data] (with-out-str (pp/pprint data)))

(defn uri [uri] (str "`" uri "`"))

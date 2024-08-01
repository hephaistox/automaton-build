(ns automaton-build.echo.headers
  "Prints text with headers on the terminal.

  Each content is modified to wrap the `width` of the terminal, and is modified to have a left margin."
  (:require
   [automaton-build.echo.common :as build-echo-common]
   [automaton-build.os.cmds     :as build-commands]
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.text     :as build-text]
   [clojure.string              :as str]))

(defn current-left-margin
  "Returns the string of the margin to add."
  []
  (let [section (get @build-echo-common/echo-param :section 0)]
    (if (nil? section) "" (apply str (repeat section \space)))))

(defn screenify
  "Prepare the `texts` strings to be printed on the screen."
  [texts]
  (let [wrapped-width (- (:width @build-echo-common/echo-param)
                         (get @build-echo-common/echo-param :section 0))
        strs (-> (str/join " " texts)
                 str/split-lines)]
    {:wrapped-width wrapped-width
     :lm-str (current-left-margin)
     :wrapped-strs (->> strs
                        (mapcat #(build-echo-common/wrap-str wrapped-width %))
                        vec)}))

(defn pure-printing
  [texts]
  (let [{:keys [lm-str wrapped-strs]} (screenify texts)]
    (doseq [l wrapped-strs] (println (str lm-str l))))
  (print build-text/font-default))

(defn normalln
  "Print text without decoration, with a carriage return included."
  [& texts]
  (print build-echo-common/normal-font-color)
  (pure-printing texts))

(defn errorln
  "To print some text that is an error, should be highlighted and rare."
  [& texts]
  (print build-echo-common/error-font-color)
  (pure-printing texts))

(defn exceptionln [e] (errorln (ex-cause e)) (println e))

(defn- header-printing
  [prefix texts]
  (let [{:keys [lm-str wrapped-strs]} (screenify texts)]
    (doseq [l wrapped-strs]
      (println (str (apply str (butlast lm-str)) prefix l)))))

(defn h1
  [& texts]
  (print build-text/font-white)
  (swap! build-echo-common/echo-param assoc :section 1)
  (header-printing "*" texts))

(defn h1-error
  [& texts]
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-red))
  (header-printing "!" texts))

(defn h1-valid
  [& texts]
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-green))
  (header-printing ">" texts))

(defn h2
  [& texts]
  (print build-text/font-white)
  (swap! build-echo-common/echo-param assoc :section 2)
  (header-printing "*" texts))

(defn h2-error
  [& texts]
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-red))
  (header-printing "!" texts))

(defn h2-valid
  [& texts]
  (print (str build-text/move-oneup build-text/clear-eol build-text/font-green))
  (header-printing ">" texts))

(comment
  (h1 "test  very long one......")
  (h1-error " failed testt")
  (h1-valid " valid test"))

(defn data
  [data]
  (-> (build-echo-common/pprint data)
      normalln))

(defn print-cmd
  "Print the command string `cmd-str` with the `prefixs` added."
  [cmd]
  (build-echo-common/print-cmd (current-left-margin) cmd))

(defn print-cmd-result
  "Print the result of a command."
  [message prefixs {:keys [exit out err]}]
  (when-not (= 0 exit)
    (errorln prefixs message)
    (when-not (str/blank? out) (normalln prefixs out))
    (when-not (str/blank? err) (normalln prefixs err))))

(defn blocking-cmd
  "Print the command `cmd` if `verbose?` is `true`, and execute it if needed.
  The `prefixs` are added, the `err-message` also if a non nil exit code is returned."
  [verbose? cmd prefixs err-message]
  (when verbose? (print-cmd cmd))
  (let [proc (build-commands/blocking-cmd cmd)]
    (print-cmd-result err-message prefixs proc)))

(defn long-living-cmd
  "Print the command `cmd` if verbose is `true`, and execute it if needed.
  The `prefixs` are added, the `erro-message` also if the error occur."
  [verbose? cmd prefixs refresh-delay]
  (when verbose? (print-cmd cmd))
  (let [proc (build-commands/create-process cmd)]
    (future (build-commands/log-stream proc
                                       :out
                                       (partial normalln prefixs)
                                       #(normalln "Terminated out process.")
                                       refresh-delay
                                       errorln))
    (future (build-commands/log-stream proc
                                       :err
                                       (partial normalln prefixs)
                                       #(normalln "Terminated err process.")
                                       refresh-delay
                                       errorln))
    (build-commands/exec proc)
    (Thread/sleep 1000)
    proc))

(defn kill [process] (build-commands/kill process))

(comment
  (def tmp (build-file/create-temp-file "test"))
  (def p
    (long-living-cmd true ["npx" "tailwindcss" "-o" tmp "--watch"] ["tst"] 100))
  (def p (long-living-cmd true ["echo" "tailwindcss"] ["tst"] 100))
  (kill p)
  ;
)

(defn uri [uri] (build-echo-common/uri uri))

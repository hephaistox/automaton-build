(ns automaton-build.echo.actions
  "Echoing in terminal for long living and possibly parrallel actions (like REPL and so on.).

  Each line is prefixed with the name of the concerned action."
  (:require
   [automaton-build.echo.common :as build-echo-common]
   [automaton-build.os.text     :as build-text]
   [clojure.string              :as str]))

(defn- prefixs-to-str
  "Turns a `prefixs` collection into a `string`."
  [prefixs]
  (str (str/join "-" prefixs) ">"))

;;Screenify
(defn screenify
  "Prepare the `texts` strings to be printed on the terminal."
  [prefixs texts]
  (let [prefix-str (prefixs-to-str prefixs)]
    (-> (build-echo-common/screenify prefix-str texts)
        (assoc :prefix-str prefix-str))))

;; Standardized echoing functions
(defn- pure-printing
  [prefixs texts]
  (let [{:keys [wrapped-strs]} (screenify prefixs texts)]
    (doseq [wrapped-str wrapped-strs] (println wrapped-str)))
  (print build-text/font-default))

(defn normalln
  "Print as normal text the collection of `texts`, with the `prefixs` added.
  It is the default printing method."
  [prefixs & texts]
  (when-not (empty? texts) (pure-printing prefixs texts)))

(defn errorln
  "Print as an error the collection of `texts`, with the `prefixs` added.
  It should be highlighted and rare (like one line red for each error and not its details)."
  [prefixs & texts]
  (print build-text/font-red)
  (pure-printing prefixs texts))

(defn exceptionln
  "Display exception `e`."
  [prefixs e]
  (errorln prefixs (ex-cause e))
  (normalln prefixs e))

(defn print-exec-cmd-str
  "Prints the execution of command string `cmd-str` with the `prefixs` added."
  [prefixs cmd-str]
  (normalln prefixs (build-echo-common/exec-cmd-str cmd-str)))

(defn print-cmd-str
  "Prints the execution of command string `cmd-str` with the `prefixs` added."
  [prefixs cmd-str]
  (normalln prefixs (build-echo-common/cmd-str cmd-str)))

;; Action specific echoing functions
(defn action
  "Print an action with its `prefixs`, the text of the action is `texts`."
  [prefixs & texts]
  (print build-text/font-green)
  (pure-printing prefixs texts))

;; Formatting helpers functions.
(defn pprint-str
  "Pretty print `data`"
  [data]
  (build-echo-common/pprint-str data))

(defn uri-str
  "Returns the string of the `uri`."
  [uri]
  (build-echo-common/uri-str uri))

(defn current-time-str
  "Returns current time string."
  []
  (build-echo-common/current-time-str))

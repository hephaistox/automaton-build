(ns automaton-build.log
  "Logger for building system
  Is a basic implementation with print

  Design decisions:
  * That log is disabled during test, which is detected with `hephaistox-in-test`
  * The log designed for the main app is optimized, this one is not, as it is much less used (only during building of the app)
  * Log levels and default values have two levels of default values:
     * In this namespace, it is the default level for the repl. As reload of the namespace, like with cider-ns-refresh will lead reload this by-default value
        * so reload of this namespace will lost any change you did since the previous loading
     * cli has its own default value, meaning that a user launching this code through the cli will be defaulted to the default values in this namespace"
  (:require
   [automaton-build.os.java-properties :as build-java-properties]
   [automaton-build.utils.string :as build-string]
   [clojure.pprint :as pp]))

(def size-command
  "Size of the command line to be managed, measured on mcbook pro"
  243)

(def ^:private log-levels [:trace :debug :info :warning :error :fatal])

(def ^:private log-level-to-idx
  (into {} (map-indexed (fn [idx itm] [itm idx]) log-levels)))

(defn compare-log-levels
  "Is the log-level greater than the reference
  Params:
  * `log-levels` collection of log levels in the order of more detailed to more scarce errors"
  [& log-levels-to-compare]
  (or (empty? log-levels-to-compare)
      (->> log-levels-to-compare
           (keep (set log-levels))
           (map log-level-to-idx)
           (apply <=))))

(def min-level "Minimum level to be displayed during log" (atom :trace))

(def details? (atom true))

(defmacro print-message
  "Helper function to print the log message"
  [level & messages]
  (when-not (build-java-properties/get-java-property "hephaistox-in-test")
    `(let [prefix# (str (.format (java.text.SimpleDateFormat. "HH:mm:ss:SSS")
                                 (java.util.Date.))
                        " "
                        ~level
                        "-"
                        (-> (ns-name *ns*)
                            str
                            (build-string/fix-length 45 "" " "))
                        "--> ")
           suffix# ""]
       (println (build-string/limit-length (str ~@messages)
                                           (if @details? 10000 size-command)
                                           prefix#
                                           suffix#
                                           (constantly nil))))))

(defn set-min-level!
  "Set the minimum level"
  [min-level*]
  (reset! min-level min-level*)
  (when (compare-log-levels @min-level :debug)
    (print-message "D"
                   (format "Log is initialized with level `%s`" min-level*))))

(defn set-details?
  "If true, the console will limit to the size"
  [b]
  (reset! details? b)
  (when (compare-log-levels @min-level :debug)
    (print-message "D"
                   (if b
                     "Log details can overflow the line"
                     "Logs are ellipsis if they overflow the line width"))))

(defn min-level-kw [] @min-level)

(defmacro trace
  [& messages]
  `(let [printable# (compare-log-levels @min-level :trace)]
     (when printable# (print-message "T" ~@messages))))

(defmacro debug
  [& messages]
  `(let [printable# (compare-log-levels @min-level :debug)]
     (when printable# (print-message "D" ~@messages))))

(defmacro info
  [& messages]
  `(let [printable# (compare-log-levels @min-level :info)]
     (when printable# (print-message "I" ~@messages))))

(defmacro warn
  [& messages]
  `(let [printable# (compare-log-levels @min-level :warning)]
     (when printable# (print-message "W" ~@messages))))

(defmacro error
  [& messages]
  `(let [printable# (compare-log-levels @min-level :error)]
     (when printable# (print-message "E" ~@messages))))

(defmacro fatal
  [& messages]
  `(let [printable# (compare-log-levels @min-level :fatal)]
     (when printable# (print-message "F" ~@messages))))

(defn format-str
  [fmt & messages]
  (try (apply format fmt messages)
       (catch Exception _
         (warn "Unexpected error during log formatting")
         (apply str fmt "-" messages))))

(defmacro trace-format
  [fmt & messages]
  `(let [printable# (compare-log-levels @min-level :trace)]
     (when printable# (print-message "T" (format-str ~fmt ~@messages)))))

(defmacro debug-format
  [fmt & messages]
  `(let [printable# (compare-log-levels @min-level :debug)]
     (when printable# (print-message "D" (format-str ~fmt ~@messages)))))

(defmacro info-format
  [fmt & messages]
  `(let [printable# (compare-log-levels @min-level :info)]
     (when printable# (print-message "I" (format-str ~fmt ~@messages)))))

(defmacro warn-format
  [fmt & messages]
  `(let [printable# (compare-log-levels @min-level :info)]
     (when printable# (print-message "W" (format-str ~fmt ~@messages)))))

(defmacro error-format
  [fmt & messages]
  `(let [printable# (compare-log-levels @min-level :error)]
     (when printable# (print-message "E" (format-str ~fmt ~@messages)))))

(defmacro fatal-format
  [fmt & messages]
  `(let [printable# (compare-log-levels @min-level :fatal)]
     (when printable# (print-message "F" (format-str ~fmt ~@messages)))))

(defmacro trace-exception
  [e]
  `(let [printable# (compare-log-levels @min-level :trace)]
     (when printable# (print-message "T" (pr-str ~e)))))

(defmacro debug-exception
  [e]
  `(let [printable# (compare-log-levels @min-level :debug)]
     (when printable# (print-message "D" (pr-str ~e)))))

(defmacro info-exception
  [e]
  `(let [printable# (compare-log-levels @min-level :info)]
     (when printable# (print-message "I" (pr-str ~e)))))

(defmacro warn-exception
  [e]
  `(let [printable# (compare-log-levels @min-level :warning)]
     (when printable# (print-message "W" (pr-str ~e)))))

(defmacro error-exception
  [e]
  `(let [printable# (compare-log-levels @min-level :error)]
     (when printable# (print-message "E" (pr-str ~e)))))

(defmacro fatal-exception
  [e]
  `(let [printable# (compare-log-levels @min-level :fatal)]
     (when printable# (print-message "F" (pr-str ~e)))))

(defmacro trace-vars
  [msg & variables]
  `(let [printable# (compare-log-levels @min-level :trace)]
     (when printable#
       (print-message "T" ~msg)
       (print-message "T"
                      (apply hash-map
                             (interleave (map symbol [~@variables])
                                         (map var-get [~@variables])))))))

(defmacro trace-map
  [msg & variables]
  `(let [printable# (compare-log-levels @min-level :trace)]
     (when printable#
       (print-message "T" ~msg)
       (print-message "T" (pp/pprint (apply hash-map [~@variables]))))))

(defmacro trace-data
  [data & messages]
  `(let [printable# (compare-log-levels @min-level :trace)]
     (when printable#
       (when-not (empty? [~@messages]) (print-message "T" ~@messages))
       (print-message "T" (pr-str ~data)))))

(defmacro debug-data
  [data & messages]
  `(let [printable# (compare-log-levels @min-level :debug)]
     (when printable#
       (when-not (empty? [~@messages]) (print-message "D" ~@messages))
       (print-message "D" (pr-str ~data)))))

(defmacro warn-data
  [data & messages]
  `(let [printable# (compare-log-levels @min-level :warning)]
     (when printable#
       (when-not (empty? [~@messages]) (print-message "W" ~@messages))
       (print-message "W" (pr-str ~data)))))

(defmacro error-data
  [data & messages]
  `(let [printable# (compare-log-levels @min-level :error)]
     (when printable#
       (when-not (empty? [~@messages]) (print-message "E" ~@messages))
       (print-message "E" (pr-str ~data)))))

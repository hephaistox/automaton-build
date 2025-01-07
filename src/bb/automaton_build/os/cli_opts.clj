(ns automaton-build.os.cli-opts
  "Parse cli options.

  Proxy to [tools.cli](https://github.com/clojure/tools.cli)"
  (:require
   [clojure.string    :as str]
   [clojure.tools.cli :as tools-cli]))

;; Definitions
(def help-options [["-h" "--help" "Print usage."]])

(def verbose-options [["-v" "--verbose" "Verbose"]])

;; Parsing
(defn parse-cli-args
  "Parse `cli-args` (defaulted to actual cli arguments.) with `cli-options.`.

  Returns a map with `[options arguments errors summary]` fields."
  ([cli-options] (tools-cli/parse-opts *command-line-args* cli-options))
  ([cli-args cli-options] (tools-cli/parse-opts cli-args cli-options)))

;; ********************************************************************************
;; Messages
;; ********************************************************************************

(defn error-msg
  "If there are errors in the parsing, returns string reporting a parsing error."
  [{:keys [errors]
    :as _parsed-cli-opts}]
  (when errors
    (str "The following errors occured while parsing your command:\n\n"
         (str/join \newline errors))))

(defn usage-msg
  "Returns the string for the summary of the task."
  [{:keys [summary]
    :as _parsed-cli-opts}
   current-task]
  (->> [(str "Usage: bb " current-task " [options]") "" "Options:" summary]
       (str/join \newline)))

(defn usage-with-arguments-msg
  "Returns the string explaining the usage of the task, with mandatory arguments."
  [{:keys [summary]
    :as _parsed-cli-opts}
   current-task
   arguments
   arguments-desc]
  (->> [(str "Usage: bb " current-task " [options] " arguments)
        ""
        "Arguments:"
        arguments-desc
        ""
        "Options:"
        summary]
       (str/join \newline)))

(comment
 ;; For an example:
 ;; (def cli-options
 ;;   [;; First three strings describe a short-option, long-option with optional
 ;;    ;; example argument description, and a description. All three are optional
 ;;    ;; and positional.
 ;;    ["-p"
 ;;     "--port PORT"
 ;;     "Port number"
 ;;     :default
 ;;     80
 ;;     :parse-fn
 ;;     #(Integer/parseInt %)
 ;;     :validate
 ;;     [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
 ;;    ["-H"
 ;;     "--hostname HOST"
 ;;     "Remote host"
 ;;     :default
 ;;     (InetAddress/getByName "localhost")
 ;;     ;; Specify a string to output in the default column in the options summary
 ;;     ;; if the default value's string representation is very ugly
 ;;     :default-desc
 ;;     "localhost"
 ;;     :parse-fn
 ;;     #(InetAddress/getByName %)]
 ;;    ;; If no required argument description is given, the option is assumed to
 ;;    ;; be a boolean option defaulting to nil
 ;;    [nil "--detach" "Detach from controlling process"]
 ;;    ["-v"
 ;;     nil
 ;;     "Verbosity level; may be specified multiple times to increase value"
 ;;      no long-option is specified, an option :id must be given
 ;;     :id
 ;;     :verbosity
 ;;     :default
 ;;     0
 ;;     ;; Use :update-fn to create non-idempotent options (:default is applied first)
 ;;     :update-fn
 ;;     inc]
 ;;    ["-f"
 ;;     "--file NAME"
 ;;     "File names to read"
 ;;     :multi
 ;;     true ; use :update-fn to combine multiple instance of -f/--file
 ;;     :default
 ;;     []
 ;;     ;; with :multi true, the :update-fn is passed both the existing parsed
 ;;     ;; value(s) and the new parsed value from each option
 ;;     :update-fn
 ;;     conj]
 ;;    ;; A boolean option that can explicitly be set to false
 ;;    ["-d" "--[no-]daemon" "Daemonize the process" :default true]
 ;;    ["-h" "--help"]])
 ;; (def cli-options
 ;;   ;; An option with a required argument
 ;;   [["-p"
 ;;     "--port PORT"
 ;;     "Port number"
 ;;     :default
 ;;     80
 ;;     :parse-fn
 ;;     #(Integer/parseInt %)
 ;;     :validate
 ;;     [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
 ;;    ;; A non-idempotent option (:default is applied first)
 ;;    ["-v" nil "Verbosity level" :id :verbosity :default 0 :update-fn inc] ; Prior to 0.4.1, you would have to use:
 ;;    ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
 ;;    ;; A boolean option defaulting to nil
 ;;    ["-h" "--help"]])
)

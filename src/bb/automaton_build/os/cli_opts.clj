(ns automaton-build.os.cli-opts
  "Parse cli options."
  (:require
   [clojure.string    :as str]
   [clojure.tools.cli :as tools-cli]))

(def help-options [["-h" "--help" "Print usage."]])

(def verbose-options [["-v" "--verbose" "Verbose"]])

(def log-options [["-l" "--log-level" "Log levels" :default :info]])

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
 ;;     ;; If no long-option is specified, an option :id must be given
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

(defn error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn parse-cli
  "Analyse `args` with `cli-options.`. (`args` is defaulted to actual cli arguments.)
  Returns a map with `[options arguments errors summary]` fields."
  ([cli-options] (tools-cli/parse-opts *command-line-args* cli-options))
  ([args cli-options] (tools-cli/parse-opts args cli-options)))

(defn opt?
  "Returns value set for `option` in the command line (analyzed with `cli-options)`."
  [cli-options option]
  (get-in (parse-cli cli-options) [:options option]))

(defn usage
  "If set, print usage"
  [{:keys [summary options]
    :as _cli-options}
   current-task]
  (when (:help options)
    (->> [(str "Usage: bb " current-task " [options]") "" "Options:" summary]
         (str/join \newline))))

(comment
  (parse-cli [] help-options)
  ; {:options {}, :arguments [], :summary "  -h, --help", :errors nil}
  (parse-cli ["-h"] help-options)
  ; {:options {:help true}, :arguments [], :summary "  -h, --help", :errors nil}
  (parse-cli ["-e"] help-options)
  #_{:options {}
     :arguments []
     :summary "  -h, --help"
     :errors ["Unknown option: \"-e\""]}
  ;
)

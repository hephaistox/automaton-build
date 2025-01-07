(ns automaton-build.os.cmd
  "Execute an external command.

  **cmd** are based on vectors of strings like `[\"ls\" \"-la\"]`

  There are three flavors to execute this command :

  * Non blocking commands: for long living and non blocking commands
  * Blocking commands: for commands with side effects for which success is only based on exit code.
  * Returning string: a blocking command which is returning simply its output so it could be parsed

  Whatever the flavor they all return a **process**, a map containing:

  * `:cmd` the command, e.g. [\"ls\" \"-la\"]
  * `:dir` the directory as provided by the user, e.g. \"\"
  * `:out-param` out parameters
  * `:err-param` err parameters
  * `:status` is a value among `:wip`, `:didnt-start`, `:success`, `:failure`
  * `:adir` the expanded directory (useful for debug), e.g. \"/User/johndoe/hephaistox\"
  * `:bb-proc` the babashka process, normally for internal use only

  * `:exception` non nil if an exception raise when command starts."
  (:require
   [automaton-build.os.filename :as build-filename]
   [babashka.process            :as p]
   [clojure.java.io             :as io]
   [clojure.string              :as str]))

(defn- to-str [cmd] (str/join " " cmd))

(defn- create-process*
  "Returns a process executing `cmd` in directory `dir`, `out` and `err` are the parameter for babshka's process creation, it could be `:string`, `:inherited`,  ..."
  [cmd dir out err]
  (let [adir (-> (if (str/blank? dir) "." dir)
                 build-filename/absolutize)]
    (merge {:cmd cmd
            :dir dir
            :out-param out
            :err-param err
            :status :wip
            :adir adir}
           (try {:bb-proc (->> cmd
                               to-str
                               (p/process (cond-> {:shutdown p/destroy-tree
                                                   :continue true
                                                   :dir adir}
                                            out (assoc :out out)
                                            err (assoc :err err))))}
                (catch Exception exception
                  {:exception exception
                   :status :didnt-start})))))

(defn- log-stream
  "During the execution of a process, log-stream will watch its execution to log all lines of the stream `stream-kw` (that could be `:err` or `:out`).

  Most of the time, you'll call this with `future` so it will be non blocking.

  * Every `delay` ms, `(on-line-fn line)` is called to detect if a new `line` exists.
  * When a new line is found, the next iteration is done immediatly, which allows to flush the streams quickly
  * When the process ends, `(on-end-fn)` is called (with no args).
  * When `on-line-fn` if `nil`, it is noop, note that it is still waiting for the end of the process to wait for `end-fn` when finished.
  * When both `on-line-fn` and `on-end-fn` are `nil`, the whole function is noop."
  [{:keys [bb-proc]
    :as process}
   stream-kw
   on-line-fn
   on-end-fn
   refresh-delay]
  (when (or on-line-fn on-end-fn)
    (let [refresh-delay (if (number? refresh-delay) refresh-delay 100)
          stream (get bb-proc stream-kw)]
      (when (fn? on-line-fn)
        (with-open [rdr (io/reader stream)]
          (binding [*in* rdr]
            (loop []
              (let [line (read-line)]
                (when (and line (fn? on-line-fn)) (on-line-fn line))
                (let [remaining-lines? (.ready rdr)
                      proc-alive? (.isAlive (:proc bb-proc))]
                  (when (and (empty? line) (not remaining-lines?)) (Thread/sleep refresh-delay))
                  (when (or remaining-lines? proc-alive?) (recur))))))))
      (when (fn? on-end-fn) (on-end-fn))))
  process)

;; ********************************************************************************
;; Non blocking command
;; ********************************************************************************

(defn non-blocking
  "Starts the execution of the command `cmd` in directory `dir` and does not wait for the end of execution.

  If the command starts successfully, it returns a `process` with status `:wip`
  If not - most probably if directory or command does not exist - it returns a `process` with `status` `:cmd-doesnt-exist`

  Next step could be `still-running?`, `wait-for` or `kill`.

  The streams are written by some futures

  Returns a `process` with `out-future` and `err-future` future."
  [cmd dir out-print-fn err-print-fn end-fn delay]
  (let [process (create-process* cmd dir nil nil)]
    (cond-> process
      (or out-print-fn end-fn) (assoc :out-future
                                      (future (log-stream process :out out-print-fn end-fn delay)))
      err-print-fn (assoc :err-future (future (log-stream process :err err-print-fn nil delay))))))

(defn still-running?
  "Returns true if the `process` is still living?"
  [process]
  (some-> (:bb-proc process)
          p/alive?))

(defn wait-for
  "Block until the end of `process`."
  [{:keys [bb-proc]
    :as process}]
  (if bb-proc
    (let [process (update process :bb-proc deref)
          exit (get-in process [:bb-proc :exit])]
      (assoc process :status (if (= 0 exit) :success :failure)))
    process))

(defn kill
  "Kill the running process `proc`."
  [{:keys [bb-proc]
    :as non-blocking-bb-process}]
  (when bb-proc
    (p/destroy-tree bb-proc)
    (assoc non-blocking-bb-process :killed? true :success? false)))

;; ********************************************************************************
;; Blocking until the end of the command
;; ********************************************************************************

(defn blocking
  "Blocks until the end of execution of the command `cmd` in directory `dir`

  Returns status:

  * `:didnt-start` if the command didn't start due to an error,
  * `:success` if the command execution has succeeded
  * `:failure` if the command execution has failed."
  [cmd dir]
  (->> (create-process* cmd dir nil nil)
       wait-for))

(defn returning-str
  "Blocks until the end of execution of the command `cmd` in directory `dir`

  Attributes `:out` and `:err` are strings that can be parsed

  Returns status:

  * `:didnt-start` if the command didn't start due to an error,
  * `:success` if the command execution has succeeded
  * `:failure` if the command execution has failed."
  [cmd dir]
  (let [process (->> (create-process* cmd dir :string :string)
                     wait-for)]
    (-> process
        (assoc :out (:out (:bb-proc process)) :err (:err (:bb-proc process))))))

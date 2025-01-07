(ns automaton-build.tasks.impl.headers.cmds
  (:require
   [automaton-build.echo.common  :as build-echo-common]
   [automaton-build.echo.headers :refer [errorln normalln uri-str]]
   [automaton-build.os.cmds      :as build-commands]
   [clojure.string               :as str]))

;; Simple echoing delegation
;; These functions are here to make simple the echoing so all displaying functions are here and no need to require also -cmd
(defn echoed-cmd
  "Wrap a command string `cmd-str` before printing to be seen as a uri in the terminal."
  [cmd-str]
  (build-echo-common/cmd-str cmd-str))

(defn exec-cmd-str
  "Returns the string of the execution of a command `cmd-str`"
  [cmd-str]
  (build-echo-common/exec-cmd-str cmd-str))

(defn kill [process] (build-commands/kill process))

(defn success "Returns `true` if the result is a success" [result] (build-commands/success result))

(defn force-dirs [cmd-chain dir] (build-commands/force-dirs cmd-chain dir))

(defn first-failing
  "Returns the result of the first failing result of a command, or the last succesful one."
  [chain-res]
  (build-commands/first-failing chain-res))

;; Printing comamnds
(defn print-cmd-str "Print the command string `cmd-str`." [cmd-str] (normalln (echoed-cmd cmd-str)))

(defn print-exec-cmd-str
  "Print a message telling the execution of the command string `cmd-str` in directory `dir`."
  [cmd-str dir]
  (normalln (exec-cmd-str cmd-str))
  (normalln "(in directory:" (uri-str dir) ")"))

;; Execute commands
(defn print-errors-if-cmd-failed
  "Print the result of a command if an error occured:

  * if the command is not found and an exception is raised,
  * or if the command is found and return a non zero exit code.

  No need to publish this function it is included in `blocking-cmd`.
  Returns `nil`."
  [{:keys [exit out err cmd-str dir]} non-zero-exit-message]
  (when-not (= 0 exit)
    (errorln non-zero-exit-message)
    (normalln "This command has failed:")
    (print-cmd-str cmd-str)
    (normalln "(in directory" (build-echo-common/uri-str dir) ")")
    (when-not (str/blank? out) (normalln out))
    (when-not (str/blank? err) (normalln err))
    nil))

(defn blocking-cmd
  "Execute a command in the blocking mode, so the result is returned when the function is returned.

  Print the command `cmd` if verbose is `true`, and execute `cmd` in directory `dir`.
  The `prefixs` are added, the `err-message` also if an error occur.

  Returns a map with:

  * :cmd-str
  * :out
  * :dir
  * :exit
  * :err "
  [cmd dir err-message verbose?]
  (let [dir (build-commands/defaulting-dir dir)
        cmd-str (build-commands/to-str cmd)]
    (when verbose? (print-exec-cmd-str cmd-str dir))
    (let [res (build-commands/blocking-cmd-str cmd-str dir)]
      (print-errors-if-cmd-failed res err-message)
      res)))

(defn long-living-cmd
  "Execute and print command `cmd` that is a long living one. So all outputs will be displayed with `prefixs.`

  As there are listeners to achieve that, the `refresh-delay` is specifying the delay between two refreshs.

  With `out-filter-fn` you can decide which lines you display or not, knowing that (constantly true) will accept them all.
  With `err-filter-fn` you can decide which lines you display or not, knowing that (constantly true) will accept them all.

  If there is no need or no will to wait for the end of the command, just call it in a future."
  [cmd dir refresh-delay verbose? out-filter-fn err-filter-fn]
  (let [cmd-str (build-commands/to-str cmd)
        dir (build-commands/defaulting-dir dir)]
    (when verbose? (print-exec-cmd-str cmd-str dir))
    (try (let [proc (build-commands/create-process cmd dir)]
           (future (build-commands/log-stream
                    proc
                    :out
                    (fn [l] (when (out-filter-fn l) (normalln l)))
                    (fn []
                      (when verbose?
                        (normalln "Execution of" (cmd-str cmd) "ended, out listener is killed.")))
                    refresh-delay
                    errorln))
           (future (build-commands/log-stream
                    proc
                    :err
                    (fn [l] (when (err-filter-fn l) (normalln l)))
                    (fn []
                      (when verbose?
                        (normalln "Execution of" (cmd-str cmd) "ended, err listener is killed.")))
                    refresh-delay
                    errorln))
           (let [res (build-commands/exec proc)]
             ;; Without this pause, some messages are lost
             (Thread/sleep 100)
             (merge res
                    {:dir dir
                     :cmd-str cmd-str
                     :proc proc})))
         (catch Exception e
           {:e e
            :dir dir
            :cmd-str cmd-str}))))

(defn simple-shell
  ([cmd] (simple-shell cmd {}))
  ([cmd args] (build-commands/simple-shell cmd args)))

(defn chain-cmds
  "Execute a chain of commands, echo the errors of the last command if if happens, with the `non-zero-exit-message`.
  If the `non-zero-exit-message` is `nil`, no error message is printed.

  Returns the chain of results."
  [cmd-chain non-zero-exit-message verbose?]
  (let [chain-res (build-commands/chain-cmds cmd-chain)
        first-failing (-> chain-res
                          build-commands/first-failing)]
    (when verbose?
      (doseq [chain-link cmd-chain]
        (print-exec-cmd-str (build-commands/to-str (first chain-link)) (second chain-link))))
    (when non-zero-exit-message (print-errors-if-cmd-failed first-failing non-zero-exit-message))
    chain-res))

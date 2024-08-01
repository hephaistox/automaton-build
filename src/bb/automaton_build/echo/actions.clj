(ns automaton-build.echo.actions
  "Print in terminal for long living and possibly parrallel actions (like REPL and so on.).

  Each line is prefixed with the name of the concerned action."
  (:require
   [automaton-build.echo.common :as build-echo-common]
   [automaton-build.os.cmds     :as build-commands]
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.text     :as build-text]
   [clojure.string              :as str]))

(defn prefixs-to-str
  "Turn a `prefixs` collection to a `string`."
  [prefixs]
  (str (str/join "-" prefixs) ">"))

(defn pure-printing
  [prefixs texts]
  (build-echo-common/pure-printing (prefixs-to-str prefixs) texts))

(defn action
  "Print an action with its `prefixs`, the text of the action is `texts`."
  [prefixs & texts]
  (print build-text/font-green)
  (pure-printing prefixs texts))

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

(defn print-cmd
  "Print the command string `cmd-str` with the `prefixs` added."
  [prefixs cmd]
  (build-echo-common/print-cmd (prefixs-to-str prefixs) cmd))

(defn print-cmd-result
  "Print the result of a command."
  [message prefixs {:keys [exit out err]}]
  (when-not (= 0 exit)
    (errorln prefixs message)
    (when-not (str/blank? out) (normalln prefixs out))
    (when-not (str/blank? err) (normalln prefixs err))))

(defn blocking-cmd
  "Print the command `cmd` if verbose is `true`, and execute it if needed.
  The `prefixs` are added, the `err-message` also if the error occur."
  [verbose? cmd prefixs err-message]
  (when verbose? (print-cmd prefixs cmd))
  (let [proc (build-commands/blocking-cmd cmd)]
    (print-cmd-result err-message prefixs proc)))

(defn long-living-cmd
  "Execute and print command `cmd` that is a long living one. So all outputs will be displayed with `prefixs.`

  As there are listeners to achieve that, the `refresh-delay` is specifying the delay between two refreshs."
  [verbose? cmd prefixs refresh-delay]
  (when verbose? (print-cmd prefixs cmd))
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

(ns automaton-build.os.cmds
  "Execute commands."
  (:require
   [automaton-build.os.filename :as build-filename]
   [babashka.process            :as    p
                                :refer [shell]]
   [clojure.java.io             :as io]
   [clojure.string              :as str]))

(defn to-str [cmd] (str/join " " cmd))

(def schema [:sequential :string])

(defn clj-parameterize
  "Turns `par` into a parameter understood by a clojure `cli`."
  [par]
  (cond
    (string? par) (str "'\"" par "\"'")
    :else par))

(defn defaulting-dir
  [dir]
  (-> (if (str/blank? dir) "." dir)
      build-filename/absolutize))

(defn simple-shell
  "Simple shell.
  Use this one when you need to leverage standard input/output for the command called."
  [cmd args]
  (shell cmd args))

(defn blocking-cmd-str
  "Returns a map with execution blocking-cmd of the blocking command `cmd-str` execution.
  Use this flavor when you need to wait for the end of the execution and have the `exit` code and outputs (`out` and `err`)."
  [cmd-str dir]
  (try (let [dir (defaulting-dir dir)
             res (when-not (str/blank? cmd-str)
                   (->> cmd-str
                        (shell (cond-> {:out :string
                                        :continue true
                                        :err :string}
                                 dir (assoc :dir dir)))))]
         {:cmd-str cmd-str
          :out (:out res)
          :dir dir
          :exit (:exit res)
          :err (:err res)})
       (catch Exception e
         {:cmd-str cmd-str
          :dir dir
          :exit -1
          :e e
          :err (or (ex-message e) "Unexpected exception")})))

(defn blocking-cmd
  "Returns a map with execution blocking-cmd of the blocking command `cmd` execution.
  Use this flavor when you need to wait for the end of the execution and have the `exit` code and outputs (`out` and `err`)."
  [cmd dir]
  (blocking-cmd-str (to-str cmd) dir))

(defn force-dirs
  "Update a chain so all element of the chain are executed in the same `dir`"
  [cmd-chain dir]
  (mapv #(assoc % 1 dir) cmd-chain))

(defn success
  "Returns `true` if the result is a success"
  [result]
  (= 0 (:exit result)))

(defn chain-cmds
  "Execute all commands in the chain, stops at the first failing one."
  [cmd-chain]
  (loop [cmd-chain cmd-chain
         result []]
    (let [rest-cmd-chains (rest cmd-chain)
          chain-link (first cmd-chain)]
      (if-not chain-link
        result
        (let [{:keys [dir cmd]} {:dir (second chain-link)
                                 :cmd (first chain-link)}
              {:keys [dir exit]
               :as res}
              (blocking-cmd cmd dir)
              chain-link-res (assoc res :cmd cmd :dir dir)]
          (if (zero? exit)
            (recur rest-cmd-chains (conj result chain-link-res))
            (-> result
                (concat [chain-link-res] (mapv chain-link-res rest-cmd-chains))
                vec)))))))

(defn first-failing
  "Returns the result of the first failing result of a command, or the last succesful one."
  [chain-res]
  (if-let [failing-res (->> chain-res
                            (remove success)
                            first)]
    failing-res
    (last chain-res)))

(defn create-process-str
  "Create a process executed in directory `dir` and based on command string `cmd-str`.

  Returns a process."
  [cmd-str dir]
  (->> cmd-str
       (p/process {:shutdown p/destroy-tree
                   :dir (defaulting-dir dir)})))

(defn create-process
  "Create a process executed in directory `dir` and based on command string `cmd-str`.

  Returns a process."
  [cmd dir]
  (create-process-str (to-str cmd) dir))

(defn kill "Kill the running process `proc`." [proc] (p/destroy-tree proc))

(defn log-stream
  "Apply `logger-fn` to each line of the stream called `stream-kw` of the `proc`. When the `proc` is not alive.
  `refresh-delay` pauses between two attempts of refreshing the log.
  If an error occur, use `error-fn` to display it."
  [proc stream-kw on-line-fn on-end-fn refresh-delay error-fn]
  (let [refresh-delay (if (number? refresh-delay)
                        refresh-delay
                        (do (error-fn
                             "unexpected error: refresh-delay is not a number:"
                             refresh-delay)
                            100))
        stream (get proc stream-kw)]
    (with-open [rdr (io/reader stream)]
      (binding [*in* rdr]
        (loop []
          (when-let [line (read-line)] (on-line-fn line))
          (let [remaining-lines? (.ready rdr)
                proc-alive? (.isAlive (:proc proc))]
            (when-not remaining-lines? (Thread/sleep refresh-delay))
            (when (or remaining-lines? proc-alive?) (recur))))))
    (on-end-fn)))

(defn exec [process] (when process (deref process)))

(ns automaton-build.os.cmds
  "Execute commands."
  (:refer-clojure :exclude [print])
  (:require
   [automaton-build.os.file :as build-file]
   [babashka.process        :as    p
                            :refer [shell]]
   [clojure.java.io         :as io]
   [clojure.string          :as str]))

(defn to-str [cmd] (str/join " " cmd))

(def schema [:sequential :string])

(defn blocking-cmd
  "Returns a map with execution blocking-cmd of the blocking command `cmd` execution.
  Use this flavor when you need to wait for the end of the execution and have the `exit` code and outputs (`out` and `err`)."
  [cmd]
  (try (let [cmd (-> cmd
                     to-str
                     build-file/expand-home-str)
             res (->> cmd
                      (shell {:out :string
                              :continue true
                              :err :string}))]
         {:cmd cmd
          :out (:out res)
          :exit (:exit res)
          :err (:err res)})
       (catch Exception e
         {:cmd cmd
          :exit -1
          :e e
          :err (ex-cause e)})))

(comment
  (def res1 (blocking-cmd ["non-existing"]))
  (def res1 (blocking-cmd ["ls"]))
  res1
  {:cmd ["ls"]
   :out "README.md\nautomaton..."
   :exit 0
   :err ""}
  (def res (blocking-cmd ["npm" "audit"]))
  res
  {:cmd ["npm" "audit"]
   :out "# npm audit report\n\nbraces..."
   :exit 1
   :err ""})

(defn create-process
  "Create a process based on command `cmd`, return a process."
  [cmd]
  (->> cmd
       to-str
       build-file/expand-home-str
       (p/process {:shutdown p/destroy-tree})))

(defn kill "Kill the running process `proc`." [proc] (p/destroy-tree proc))

(defn log-stream
  "Apply `logger-fn` to each line of the stream called `stream-kw` of the `proc`. When the `proc` is not alive.
  `refresh-delay` pauses between two attempts of refreshing the log.
  If an error occur, print it with `error-fn`"
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

(comment
  (def tmp (build-file/create-temp-file "test"))
  (def p (create-process ["npx" "tailwindcss" "-o" tmp "--watch"]))
  (future (log-stream p
                      :out
                      #(println "out:" %)
                      #(println "log is killed.")
                      100
                      println))
  (future (log-stream p
                      :err
                      #(println "err:" %)
                      #(println "err log is killed.")
                      100
                      println))
  @p
  p
  (kill p)
  (def p2
    (create-process ["npx"
                     "shadow-cljs"
                     "watch"
                     "ltest"
                     "landing-app"
                     "browser-test"
                     "portfolio"]))
  (future (log-stream p2
                      :out
                      #(println "out:" %)
                      #(println "log is killed.")
                      100
                      println))
  (future (log-stream p2
                      :err
                      #(println "err:" %)
                      #(println "err log is killed.")
                      100
                      println))
  ;
)

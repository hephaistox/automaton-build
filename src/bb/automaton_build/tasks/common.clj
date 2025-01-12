(ns automaton-build.tasks.common
  "All common workflow features."
  (:require
   [automaton-build.os.cli-opts   :as build-cli-opts]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn enter
  "When entering a task:

  * Print usage if required.
  * Print options if required."
  [cli-opts current-task]
  (when-let [message (build-cli-opts/error-msg cli-opts)]
    (println message)
    (println)
    (println (build-cli-opts/usage-msg cli-opts (:name current-task)))
    (build-exit-codes/exit build-exit-codes/command-not-found))
  (when (get-in cli-opts [:options :help])
    (when (get-in cli-opts [:options :verbose])
      (println "Options are:")
      (println (pr-str cli-opts)))
    (println (build-cli-opts/usage-msg cli-opts (:name current-task)))
    (build-exit-codes/exit build-exit-codes/ok)))

(defn enter-with-arguments
  "When entering the task:

  * Print usage if required.
  * Print options if required."
  [cli-opts
   current-task
   {:keys [doc-str message valid-fn]
    :as _arguments}]
  (when (or (not (fn? valid-fn)) (not (valid-fn (:arguments cli-opts))))
    (println "Arguments are not valid.")
    (println)
    (println
     (build-cli-opts/usage-with-arguments-msg cli-opts (:name current-task) doc-str message))
    (build-exit-codes/exit build-exit-codes/invalid-argument))
  (when-let [message (build-cli-opts/error-msg cli-opts)]
    (println message)
    (println)
    (println (build-cli-opts/usage-msg cli-opts (:name current-task)))
    (build-exit-codes/exit build-exit-codes/command-not-found))
  (when (get-in cli-opts [:options :help])
    (when (get-in cli-opts [:options :verbose])
      (println "Options are:")
      (println (pr-str cli-opts)))
    (println (build-cli-opts/usage-msg cli-opts (:name current-task)))
    (build-exit-codes/exit build-exit-codes/ok)))

(ns automaton-build.os.cli-input
  (:require
   [automaton-build.os.terminal-msg :as build-terminal-msg]))

(defn user-input "Reads user input" [] (read))

(defn yes-question
  "Asks user a `msg` and expects yes input. Returns true or false based on the response."
  ([msg force?]
   (if force?
     true
     (do (build-terminal-msg/println-msg msg)
         (flush)
         (contains? #{'y 'Y 'yes 'Yes 'YES} (user-input)))))
  ([msg] (yes-question msg false)))

(defn question-loop
  ([msg options force?]
   (if force?
     nil
     (let [answer (atom true)]
       (while (not (some #(= @answer %) options))
         (build-terminal-msg/println-msg msg)
         (flush)
         (reset! answer (user-input)))
       @answer)))
  ([msg options] (question-loop msg options false)))
(ns automaton-build.os.cli-input
  (:require
   [automaton-build.os.terminal-msg :as build-terminal-msg]))

(defn user-input "Reads user input" [] (read))

(defn user-input-str
  "Return string from user input
   For unknown reason read-line not always asks for input, so additional check needs to be done."
  []
  (let [answer (read-line) answer-repeat (if (= answer "") (read-line) answer)] answer-repeat))

(defn question
  ([msg force?] (if force? true (do (build-terminal-msg/println-msg msg) (flush) (user-input))))
  ([msg] (question msg false)))

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
     (loop []
       (build-terminal-msg/println-msg msg)
       (flush)
       (let [answer (user-input-str)] (if (some #(= answer %) options) answer (recur))))))
  ([msg options] (question-loop msg options false)))

(ns automaton-build.os.cli-input)

(defn user-input "Reads user input" [] (read))

(defn user-input-str
  "Return string from user input
   For unknown reason read-line not always asks for input, so additional check needs to be done."
  []
  (let [answer (read-line) answer-repeat (if (= answer "") (read-line) answer)] answer-repeat))

(defn question
  ([{:keys [normalln]
     :as _printers}
    msg
    force?]
   (if force? true (do (normalln msg) (flush) (user-input))))
  ([printers msg] (question printers msg false)))

(defn yes-question
  "Asks user a `msg` and expects yes input. Returns true or false based on the response."
  ([{:keys [normalln]
     :as _printers}
    msg
    force?]
   (if force? true (do (normalln msg) (flush) (contains? #{'y 'Y 'yes 'Yes 'YES} (user-input)))))
  ([printers msg] (yes-question printers msg false)))

(defn question-loop
  ([{:keys [normalln]
     :as _printers}
    msg
    options
    force?]
   (if force?
     nil
     (loop []
       (normalln msg)
       (flush)
       (let [answer (user-input-str)] (if (some #(= answer %) options) answer (recur))))))
  ([printers msg options] (question-loop printers msg options false)))

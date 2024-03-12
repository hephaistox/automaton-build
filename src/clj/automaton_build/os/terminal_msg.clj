(ns automaton-build.os.terminal-msg
  "Print message on the terminal - even if the log is activated and routed somewhere else.")

(defn println-msg
  "Display a regular message on the terminal"
  [& msg]
  (apply println msg))

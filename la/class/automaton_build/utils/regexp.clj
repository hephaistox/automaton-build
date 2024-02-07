(ns automaton-build.utils.regexp)

(defn all-white-spaces
  "Finds all whitespaces and all whitespace special characters (e.g. \n \r, tabs, spaces)."
  []
  #"(\p{C})|(\s)")

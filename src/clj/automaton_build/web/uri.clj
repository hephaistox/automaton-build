(ns automaton-build.web.uri "Manages uri")

(defn from-file-path
  "Adds `file:` as a prefix to the string. Usefull when java io resource type of path is needed"
  [path]
  (str "file:" path))

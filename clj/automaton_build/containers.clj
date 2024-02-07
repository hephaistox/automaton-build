(ns automaton-build.containers "Defines container expectations")

(defprotocol Container
  (build [this publish?]
   "Build the image\n Params:\n * `publish?` if true publish the image\n Return true or nil if any error")
  (connect [this]
   "Connect to a local copy of that container")
  (container-name [this]
   "name for the local engine and remote repo")
  (container-tagged-name [this]
   "name with tag for the local engine and remote repo"))

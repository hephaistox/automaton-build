(ns automaton-core.utils.uuid-gen
  "Generate uuid, is a proxy to `http://danlentz.github.io/clj-uuid/`"
  (:require
   [clj-uuid :as uuid]))

(defn time-based-uuid
  "Generate a time based uuid, so sorting uuid is sorting chronologically"
  []
  (uuid/v1))

(defn unguessable
  "When the uuid should not be guessed"
  []
  (uuid/v4))

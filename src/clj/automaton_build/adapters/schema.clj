(ns automaton-build.adapters.schema
  "Validate the data against the schema.
  Is a proxy for malli"
  (:require
   [malli.core :as m]
   [malli.error :as me]))

(defn schema-valid
  "Return true if the data is not matching the schema
  Params:
  * `schema` schema to match
  * `data` data to check appliance to schema"
  [schema data]
  (m/validate schema
              data))

(defn schema-valid-or-throw
  "True or throw an exception if the data is not matching the schema.
  Params:
  * `schema` schema to match
  * `data` data to check appliance to schema
  * `msg` to send in the exception if thrown"
  [schema data msg]
  (when-not (schema-valid schema
                          data)
    (throw (ex-info msg
                    {:data data
                     :reason (-> schema
                                 (m/explain data)
                                 (me/humanize))})))
  data)

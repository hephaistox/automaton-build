(ns automaton-build.schema
  "Validate the data against the schema.
  Is a proxy for malli"
  (:require
   [automaton-build.log :as build-log]
   [malli.core          :as malli]
   [malli.error         :as malli-error]
   [malli.transform     :as malli-transform]))

(defn valid?
  "Returns data if the data is matching the schema
  Return nil otherwise
  Params:
  * `schema` schema to match
  * `data` data to check appliance to schema
  * `data-name` the name of the data tested, as printed in the log if not valid"
  [schema data data-name]
  (if (malli/validate schema data)
    data
    (do (build-log/warn-format "`%s` is not validated %s"
                               data-name
                               (-> schema
                                   (malli/explain data)
                                   (malli-error/humanize)))
        (build-log/warn-data (pr-str data))
        nil)))

(defn add-default-values
  "Add the default values of the schema
  Returns data augmented with the default values,
  Params:
  * `schema`
  * `data`"
  [schema data]
  (malli/decode schema
                data
                (malli-transform/default-value-transformer {::malli-transform/add-optional-keys
                                                            true})))

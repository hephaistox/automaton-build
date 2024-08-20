(ns automaton-build.data.schema
  "Validate the data against the schema.

  Proxy to [malli](https://github.com/metosin/malli)."
  (:require
   [clojure.pprint  :as pp]
   [malli.core      :as malli]
   [malli.error     :as malli-error]
   [malli.transform :as malli-transform]))

(defn valid?
  "Returns `true` if `data` is matching the `schema`."
  [schema data]
  (malli/validate schema data))

(defn humanize
  "Returns a vector of string of humanized messages for `data` compliance to `schema`.
  Returns `nil` if no error is found."
  [schema data]
  (let [h (some-> schema
                  (malli/explain data)
                  (malli-error/humanize))]
    (when h (with-out-str (pp/pprint h)))))

(defn add-default-values
  "Returns `data` augmented with the default values, as defined in the `schema`."
  [schema data]
  (malli/decode schema
                data
                (malli-transform/default-value-transformer
                 {::malli-transform/add-optional-keys true})))

(defn validate-with-defaults
  "Returns `true` if `data` is compliant to the `schema`, knowing its defaults."
  [schema data]
  (->> data
       (add-default-values schema)
       (valid? schema)))

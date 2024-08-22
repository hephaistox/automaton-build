(ns automaton-build.os.edn-utils-bb
  "Adapter to read an edn file."
  (:require
   [automaton-build.os.file :as build-file]
   [clojure.edn             :as edn]))

(defn str->edn [raw-content] (edn/read-string raw-content))

(defn read-edn
  "Read file which name is `edn-filename`.

  Returns:

  * `filename`
  * `raw-content` if file can be read.
  * `invalid?` to `true` whatever why.
  * `exception` if something wrong happened.
  * `edn` if the translation."
  [edn-filename]
  (let [{:keys [raw-content invalid?]
         :as res}
        (build-file/read-file edn-filename)]
    (if invalid?
      res
      (try (assoc res :edn (str->edn raw-content))
           (catch Exception e (assoc res :exception e :invalid? true))))))

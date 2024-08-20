(ns automaton-build.code.reports "Reports analyzing the clojure code.")

(defn is-ignored-file?
  [file-content]
  (re-find #":heph-ignore \{:reports false\}" file-content))

(defn search-aliases
  "Returns a list of matches, each one being a `filename`, a `ns` and an `alias`."
  [{:keys [raw-content filename]
    :as _file-desc}]
  (when raw-content
    (let
      [matcher
       (->
         #"(?x)\[\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)\s*(?:(?::as|:as-alias)\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)|(:refer).*)\s*\]"
         (re-matcher raw-content))]
      (loop [matches []]
        (let [[_ ns alias] (re-find matcher)]
          (if (nil? ns)
            matches
            (recur (conj matches
                         {:filename filename
                          :ns ns
                          :alias alias}))))))))

(defn ns-inconsistent-aliases
  "Detect in `matches` the ones related to one namespace and two aliases or more."
  [matches]
  (->> matches
       (group-by :ns)
       (filterv (fn [[_ns ns-matches]]
                  (< 1
                     (->> ns-matches
                          (mapv :alias)
                          distinct
                          count))))
       (into {})))

(defn alias-inconsistent-ns
  "Detect in `matches` the ones related to one alias and two namespace or more."
  [matches]
  (->> matches
       (group-by :alias)
       (filterv (fn [[_ns ns-matches]]
                  (< 1
                     (->> ns-matches
                          (mapv :ns)
                          distinct
                          count))))))


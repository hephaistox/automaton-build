(ns automaton-build.utils.namespace
  "Helpers for namespace"
  (:require
   [clojure.string :as str]
   [automaton-build.os.files :as build-files]
   [automaton-build.log :as build-log]))

(defn namespaced-keyword
  "Create a namespaced keyword"
  [ns kw]
  (->> [(when-not (nil? ns) (name ns)) (when-not (nil? kw) (name kw))]
       (filterv some?)
       (str/join "/")
       symbol))

(defn update-last
  [& v]
  (let [v (vec v)] (when-not (empty? v) (conj (pop v) (str (last v) ".clj")))))

(defn ns-to-file
  "Transform a symbol of a namespace to its filename"
  [ns]
  (->> (-> (name ns)
           (str/replace #"-" "_")
           (str/split #"\."))
       (apply update-last)
       (apply build-files/create-file-path)))

(defn require-ns
  "Require the namespace of the body-fn
  Params:
  * `f` is a function full qualified symbol. It could be a string"
  [f]
  (some-> f
          symbol
          namespace
          symbol
          require))

(defn qualified-name
  "Return the qualified name of a function"
  [s]
  (-> (apply str (interpose "/" ((juxt namespace name) (symbol s))))))

(defn symbol-to-fn-call
  "Resolve the symbol and execute the associated function
  Returns nil if fn can't be called or the result of the function call, with `args` as arguments, with no surprise !`"
  [f & args]
  (if-let [res (try (require-ns f)
                    (some-> f
                            resolve)
                    (catch Exception e
                      (build-log/warn-exception
                       (ex-info "Unable to turn symbol to fn" {:symbol f} e))
                      nil))]
    (try (apply res args)
         (catch Exception e
           (build-log/warn-exception (ex-info "Unable to execute fn"
                                              {:fn f
                                               :args args}
                                              e))
           nil))
    (do (build-log/warn-format "No valid function passed, (i.e %s)" f) nil)))

(defn namespace-in-same-dir
  "Returns the namespace called `sub-ns` and stored in the same dir

  Params:
  * `ns` namespace to use as a reference to detect the directory. Could be a function in the namespace also
  * `sub-ns` sub namespace string"
  [ns sub-ns]
  (str/join "."
            (-> (str/split (str ns) #"\.")
                butlast
                vec
                (conj sub-ns))))

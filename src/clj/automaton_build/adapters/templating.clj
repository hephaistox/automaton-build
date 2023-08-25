(ns automaton-build.adapters.templating
  "Adapter to templating of documents
  Is a proxy to str/replace"
  (:require
   [clojure.string :as str]

   [automaton-core.adapters.log :as log]
   [automaton-core.adapters.edn-utils :as edn-utils]
   [automaton-core.adapters.files :as files]))

(defn get-keys
  "Return a vector of the keys, transformed in string surrounded by `prefix` and `suffix`
  Params:
  * `render-params` the set of parameters to render, the key is a keyword
  * `prefix` prefix of the kw
  * `suffix` suffix of the kw"
  [render-params prefix suffix]
  (mapv (comp #(str prefix % suffix)
              name first)
        render-params))

(defn kw-to-replacement
  "Change the keyword to a string
  Params:
  * `k` keyword to replace"
  [k]
  (name k))

(defn replacement-pattern
  "Build the replacement pattern from `:render-params` map
  Params:
  * `render-params` the map of keys to be transformed into their values
  * `prefix` prefix of the kw
  * `suffix` suffix of the kw"
  [render-params prefix suffix]
  (let [k (get-keys render-params prefix suffix)]
    [(str/join "|" k)
     (into {}
           (map (fn [[k v]]
                  [(str prefix (kw-to-replacement k) suffix) (str v)])
                render-params))]))

(defn render-content
  "Return the `content` rendered (i.e. keys from map `render-params` are replaced with their value)
  Params:
  * `content` is the string of the content of the text file to render
  * `render-params` the map of keys to be transformed into their values
  * `prefix` and `suffix` of the keywords of render  "
  [content render-params prefix suffix]
  (try
    (let [[k values] (replacement-pattern render-params prefix suffix)
          search (re-pattern k)]
      (str/replace content
                   search
                   values))
    (catch Exception e
      (log/warn "Impossible to render the template")
      (log/warn (edn-utils/spit-in-tmp-file {:exception e
                                             :content content
                                             :render-params render-params}))
      content)))

(defn render
  "For each content in `files`, the keys in `render-values` are replaced with their values
  Parameters are:
  * `files` is a vector of [`filename` and `file-content`]
  * `render-values` is a map where keys are replaced with values
  * `prefix` and `suffix` are added to the key during the search"
  [files render-values prefix suffix]
  (doseq [[filename file-content] files]
    (let [original-content file-content
          rendered-content (render-content original-content
                                           render-values
                                           prefix suffix)]
      (files/spit-file filename rendered-content))))

(defn escape-minus
  "Add an escape to minus symbol
  Params:
  * `txt` string in which `-` are escaped"
  [txt]
  (str/replace txt
               #"-"
               (str/re-quote-replacement "\\-")))

(defn search-tokens
  "Return the list of tokens found in the files content
  A token is made
  Params:
  * `files` is the list of files to search in, a map associating filenames to its content
  * `prefix-delimiter` and `suffix-delimiter` are the values searched for in each file. All string in between is called a token"
  [files prefix-delimiter suffix-delimiter]
  (let [pattern (re-pattern (str (escape-minus prefix-delimiter)
                                 "([\\w\\-]*)"
                                 (escape-minus suffix-delimiter)))
        mustache-tokens (disj (into #{}
                                    (map (fn [[_ file]]
                                           (last
                                            (re-find pattern
                                                     file)))
                                         files))
                              nil)]
    (into {}
          (map (fn [token-to-set]
                 [(keyword token-to-set) ""])
               mustache-tokens))))

(defn change-marker
  "The template files with first lines containing the marker
  Params:
  * `file-content` content of the file
  * `marker` the marker to research, is used in a replace
  * `new-marker` what to replace that string with"
  [file-content marker new-marker]
  (let [splited-content (str/split-lines file-content)
        first-line (first splited-content)]
    (when (re-find marker first-line)
      (str/join "\n"
                (conj (rest splited-content)
                      (str/replace first-line
                                   marker
                                   new-marker))))))

(defn rename-dirs
  "Rename the directories"
  [files pattern new-pattern]
  (doseq [f files]
    (let [f (str f)
          new-f (str/replace f (re-pattern pattern)
                             new-pattern)]
      (when (and (files/is-existing-file? f)
                 (not= f new-f))
        (files/rename-file f new-f)))))

(comment
  ;; Rename files with mustache extensions to file for which mustache is the suffix of the name of the file
  (map (fn [f]
         (let [f (str f)
               new-f (str/replace f #"template-app"
                                  "template_app")]
           (when (and (files/is-existing-file? f)
                      (not= f new-f))
             (files/rename-file f new-f))))
       (files/search-files "template_app"
                           "**"))

;; Rename the tokens in the file from {{target-app}} to footemplate-appfoo
  (map (fn [f]
         (let [file (str f)]
           (when (files/is-existing-file? file)
             (let [file-updated  (-> (slurp file)
                                     (str/replace #"chewi-(.*)-hansolo"
                                                  "$1"))]

               (spit file file-updated)))))
       (files/search-files "template_app"
                           "**"))
  ;;
  )

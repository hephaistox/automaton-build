(ns automaton-build.code.reports "Create reports analyzing the clojure code.")

(defn search-aliases
  "Search aliases in the `file-content`.

  Returns a map with `filename`, `ns`, `alias`."
  [filename file-content]
  (let
    [matcher
     (->
       #"(?x)\[\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)\s*(?:(?::as|:as-alias)\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)|(:refer).*)\s*\]"
       (re-matcher file-content))]
    (loop [matches []]
      (let [[_ ns alias] (re-find matcher)]
        (if (nil? ns)
          matches
          (recur (conj matches
                       {:filename filename
                        :ns ns
                        :alias alias})))))))

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
                          count))))))

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

(defonce T (str "T" "O" "D" "O"))

(defonce D (str "D" "O" "N" "E"))

(defonce N (str "N" "O" "T" "E"))

(defonce F (str "F" "I" "X" "M" "E"))

(defn comments
  "Search notes in the  `file-content`."
  [file-content]
  (let [matcher (-> (format "(?x);;\\s*(?:%s|%s|%s|%s)(.*)\\n" T N D F)
                    re-pattern
                    (re-matcher file-content))]
    (loop [matches []]
      (let [[msg] (re-find matcher)]
        (if (some? msg) (recur (conj matches msg)) matches)))))

(comment
  (def example
    "(ns automaton-build.wf.tests
  \"Common tasks fn for testing.\"
  (:require
   [automaton-build.app-data.repo-structure :as build-repo-structure]
   [automaton-build.code.files              :as build-code-files]
   [automaton-build.code.lint               :as build-code-lint]
   [automaton-build.code.reports            :as build-code-reports]
   [automaton-build.echo.headers            :as build-echo-headers]
   [automaton-build.os.cmds                 :as build-commands]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.wf.common               :as build-wf-common]))

  (def commit-message [\"-c\" \"--commit-message\" \"Commit message\"")
  ;
  (comments example)
  (search-aliases "xx.clj" example)
  (-> [{:filename "xx.clj"
        :ns "automaton-build.file-repo.raw"
        :alias "build-filerepo-raw"}
       {:filename "xx.clj"
        :ns "clojure.string"
        :alias "build-string"}
       {:filename "yy.clj"
        :ns "clojure.string"
        :alias "build-string"}]
      ns-inconsistent-aliases)
  [["clojure.string"
    [{:filename "xx.clj"
      :ns "clojure.string"
      :alias "build-string"}
     {:filename "yy.clj"
      :ns "clojure.string"
      :alias "str"}]]]
  ;
)

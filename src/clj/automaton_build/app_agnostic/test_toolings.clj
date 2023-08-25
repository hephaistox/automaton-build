(ns automaton-build.app-agnostic.test-toolings
  "Tooling for tests the current codebase: linters, patterns, execute tests, and so on"
  (:require
   [automaton-build.adapters.commands :as cmds]
   [automaton-build.adapters.code-files :as code-files]
   [automaton-build.adapters.log :as log]
   [automaton-build.adapters.edn-utils :as edn-utils]))

(defn unit-test
  "Run unit test"
  [dir]
  (log/trace (cmds/exec-cmds [[["clojure" "-M:runner"]]]
                             {:dir dir
                              :out :string})))

(defn unit-test-gha
  "Run unit test on github action
  Params:
  * `dir` where the tests should be run
  * `force?` if true, the tests are done locally, not on githubaction"
  [dir force?]
  (log/trace (cmds/exec-cmds [[(into []
                                     (concat ["clojure"]
                                             (when-not force?
                                               ["-Sdeps" "{:mvn/local-repo \"/usr/.mvn2\"}"])
                                             ["-M:runner"]))]]
                             {:dir dir
                              :out :string})))

(defn search-line
  "Proxy to code-files search-line function"
  [pattern line]
  (code-files/search-line pattern line))

(def comment-pattern
  #";;\s*(?:TODO|NOTE|DONE|FIXME)(.*)$")

(defn assert-comments
  "Check annotations, sends an exception if an annotation is found in all files which extension is `clj` `cljs` `cljc` or `edn`
  Mustache extensions are delibaretly excluded to add some TODO in the templates"
  [code-files-repo]
  (into {}
        (-> code-files-repo
            (code-files/exclude-files #{"test_toolings_test.clj"})
            (code-files/create-report comment-pattern)
            (code-files/map-report (fn [filename [_whole-match comment]]
                                     [comment filename]))
            (code-files/save-report "List of TODO FIXME NOTE and DONE comments")
            (code-files/assert-empty "Found forbidden comments in the code"))))

(def css-pattern
  #":class\s*\"|:(a|abbr|acronym|address|applet|area|article|aside|audio|b|automaton|basefont|bdi|bdo|big|blockquote|body|br|button|canvas|caption|center|cite|code|col|colgroup|data|datalist|dd|del|details|dfn|dialog|dir|div|dl|dt|em|embed|fieldset|figcaption|figure|font|footer|form|frame|frameset|h1|h2|h3|h4|h5|head|header|hr|html|i|iframe|img|input|ins|kbd|label|legend|li|link|main|map|mark|meta|meter|nav|noframes|noscript|object|ol|optgroup|option|output|p|param|picture|pre|progress|q|rp|rt|ruby|s|samp|script|section|select|small|source|span|strike|strong|style|sub|summary|sup|svg|table|tbody|td|template|textarea|tfoot|th|thead|time|title|tr|track|tt|u|ul|var|video|wbr)(?:\#|\.)")

(defn assert-css
  "Check annotations, sends an exception if an annotation is found in all files which extension is `clj` `cljs` `cljc` or `edn`
  Mustache extensions are delibaretly excluded to add some TODO in the templates"
  [code-files-repo]
  (into {}
        (-> code-files-repo
            (code-files/exclude-files #{"test_toolings_test.clj"})
            (code-files/create-report css-pattern)
            (code-files/map-report (fn [filename [_whole-match comment]]
                                     [comment filename]))
            (code-files/save-report "List of forbidden css forms")
            (code-files/assert-empty "Found forbidden css code"))))

(defn generate-lint-all-cmds
  "Command to lint all clojure files
  Params:
  * `code-files-repo` Directories to lint"
  [code-files-repo]
  [[(apply vector "clj-kondo"
           (mapcat (fn [[filename _]]
                     ["--lint" filename])
                   code-files-repo))]])

(defn lint-all
  "Lint all files in the clojure project
  Params:
  * `code-files-repo` the list of directories to lint"
  [code-files-repo]
  (let [cmds (generate-lint-all-cmds code-files-repo)]
    (try
      (log/trace (cmds/exec-cmds cmds
                                 {:dir "."
                                  :out :string}))
      (catch Exception e
        (log/fatal "Errors during linting:\n" (:out (ex-data (:exception (ex-data e)))))
        (throw (ex-info "Unexpected error during linting:"
                        {:exception e
                         :command (with-out-str
                                    (println (flatten cmds)))}))))))

(def alias-pattern
  "Regexp pattern to search for aliases"
  #"^\s*\[\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)\s*(?:(?::as)\s*([A-Za-z0-9\*\+\!\-\_\.\'\?<>=]*)|(:refer).*)\s*\]\s*(?:\)\))*$")

(defn namespace-report
  "Creates the list of namespaces, their alias and filenames, save it in a report"
  [code-files-repo]
  (into {}
        (-> code-files-repo
            code-files/get-clj*-files
            (code-files/create-report alias-pattern)
            (code-files/filter-report (fn [_namespace [_whole-match _namespace alias refer?]]
                                        (not (or (= refer?
                                                    ":refer")
                                                 (= "sut"
                                                    alias)))))
            (code-files/map-report (fn [filename [_whole-match namespace alias _]]
                                     [namespace filename alias]))
            (code-files/group-by-report (fn [[namespace _filename alias]]
                                          [namespace alias])
                                        (fn [[_namespace filename _alias]]
                                          filename)
                                        [])
            (code-files/group-by-report (fn [[namespace _alias _filenames]]
                                          [namespace])
                                        (fn [[_namespace alias filenames]]
                                          [alias filenames])
                                        {})
            (code-files/save-report "List of namespaces, group by alias usage:"))))

(defn namespace-has-one-alias
  "Report namespaces with more than one alias"
  [code-files-repo]
  (into {}
        (-> code-files-repo
            namespace-report
            (code-files/filter-report (fn [_namespace report-ns]
                                        (> (count report-ns)
                                           1)))
            (code-files/print-report (fn [[namespace report-ns]]
                                       (log/info (format "The namespace `%s` has too many aliases %s" namespace (keys report-ns)))
                                       (log/trace (edn-utils/spit-in-tmp-file {:namespace namespace
                                                                               :report-ns report-ns}))))
            #_(code-files/assert-empty "Some namespaces have more than one alias"))))

(defn alias-report
  "Creates the list of aliases, save it in a report"
  [code-files-repo]
  (into {}
        (-> code-files-repo
            code-files/get-clj*-files
            (code-files/create-report alias-pattern)
            (code-files/filter-report (fn [_namespace [_whole-match _namespace alias refer?]]
                                        (not (or (= refer?
                                                    ":refer")
                                                 (= "sut"
                                                    alias)))))
            (code-files/map-report (fn [filename [_whole-match namespace alias _]]
                                     [namespace filename alias]))
            (code-files/group-by-report (fn [[namespace _filename alias]]
                                          [namespace alias])
                                        (fn [[_namespace filename _alias]]
                                          filename)
                                        [])
            (code-files/group-by-report (fn [[_namespace alias _filenames]]
                                          [alias])
                                        (fn [[namespace _alias filenames]]
                                          [namespace filenames])
                                        {})
            (code-files/save-report "List of aliases, group by namespace usage:"))))

(defn alias-has-one-namespace
  "Report aliases with more than one namespace"
  [code-files-repo]
  (into {}
        (-> code-files-repo
            alias-report
            (code-files/filter-report (fn [_alias report-ns]
                                        (> (count report-ns)
                                           1)))
            (code-files/print-report (fn [[namespace report-ns]]
                                       (log/info (format "The alias `%s` has too many namespaces %s" namespace (keys report-ns)))
                                       (log/trace (edn-utils/spit-in-tmp-file {:namespace namespace
                                                                               :report-ns report-ns}))))
            #_(code-files/assert-empty "More than one alias for each namespace"))))

(ns automaton-build.tasks.reports
  (:require
   [automaton-build.app-data :as build-app-data]
   [automaton-build.code-helpers.analyze.alias-has-one-namespace
    :as
    build-analyze-alias]
   [automaton-build.code-helpers.analyze.comments :as build-analyze-comments]
   [automaton-build.code-helpers.analyze.css :as build-analyze-css]
   [automaton-build.code-helpers.analyze.forbidden-words
    :as
    build-forbidden-words]
   [automaton-build.code-helpers.analyze.namespace-has-one-alias
    :as
    build-analyze-namespace]
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.file-repo.clj-code :as build-clj-code]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files :as build-files]))

(defn- alias-report
  [{:keys [alias-outputfilename]} code-repo]
  (let [matches (build-analyze-alias/alias-matches code-repo)]
    (-> (build-analyze-alias/save-report matches alias-outputfilename)
        (build-analyze-alias/assert-empty alias-outputfilename))))

(defn- comments-report
  [{:keys [comments-outputfilename]} code-repo]
  (let [matches (build-analyze-comments/comment-matches code-repo)]
    (-> (build-analyze-comments/save-report matches comments-outputfilename)
        (build-analyze-comments/assert-empty comments-outputfilename))))

(defn- css-report
  [{:keys [css-outputfilename]} code-repo]
  (let [matches (build-analyze-css/css-matches code-repo)]
    (-> (build-analyze-css/save-report matches css-outputfilename)
        (build-analyze-css/assert-empty css-outputfilename))))

(defn forbidden-words-report
  [{:keys [forbiddenwords-words forbiddenwords-outputfilename]} clj-repo]
  (let [regexp (build-forbidden-words/coll-to-alternate-in-regexp
                forbiddenwords-words)
        matches (some-> regexp
                        (build-forbidden-words/forbidden-words-matches
                         clj-repo))]
    (-> (build-forbidden-words/save-report matches
                                           forbiddenwords-outputfilename)
        (build-forbidden-words/assert-empty forbiddenwords-outputfilename))))

(defn- namespace-report
  [{:keys [namespace-outputfilename]} clj-repo]
  (let [matches (build-analyze-namespace/alias-matches clj-repo)]
    (-> (build-analyze-namespace/save-report matches namespace-outputfilename)
        (build-analyze-namespace/assert-empty namespace-outputfilename))))

(defn- shadow-report
  [{:keys [shadow-report-outputfilename]} app-dir]
  (build-frontend-compiler/create-size-optimization-report
   app-dir
   shadow-report-outputfilename))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Build all the reports"
  [_task-map
   {:keys [app-dir]
    :as app-data}]
  (let [code-repo (->> app-data
                       build-app-data/src-dirs
                       (filter build-files/is-existing-dir?)
                       build-clj-code/make-clj-repo-from-dirs)
        res [(shadow-report app-data app-dir)
             (namespace-report app-data code-repo)
             (comments-report app-data code-repo)
             (css-report app-data code-repo)
             (alias-report app-data code-repo)
             (namespace-report app-data code-repo)
             (forbidden-words-report app-data code-repo)]]
    (if-not (every? nil? res)
      (do (build-log/error "Reports have failed") build-exit-codes/catch-all)
      build-exit-codes/ok)))

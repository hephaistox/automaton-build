(ns automaton-build.tasks.impl.linter
  "In a workflow, lint a project."
  (:require
   [automaton-build.code.lint               :as build-code-lint]
   [automaton-build.echo.headers            :refer [build-writter
                                                    errorln
                                                    h1
                                                    h1-error
                                                    h1-valid
                                                    h2-error!
                                                    h2-valid!
                                                    normalln
                                                    print-writter]]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.project.deps            :as build-deps]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd]]))

(defn lint
  "For a project which `deps.edn` is described with `deps-file-desc`.

  The files to lint are extracted fro mthe `paths` and `extra-paths` of the `deps.edn` file.
  Returns `true` if ok."
  [deps-file-desc verbose?]
  (h1 "Linter")
  (let [dir (:app-dir deps-file-desc)
        linter-status (let [paths-to-lint (->> deps-file-desc
                                               :edn
                                               build-deps/get-src
                                               (keep build-file/is-existing-dir?))]
                        (if (empty? paths-to-lint)
                          (do (normalln "Nothing to lint.") nil)
                          (let [s (build-writter)
                                lint-cmd (build-code-lint/lint-cmd verbose? paths-to-lint)
                                {:keys [out err exit]} (binding [*out* s]
                                                         (blocking-cmd lint-cmd dir "" verbose?))]
                            (cond
                              (zero? exit) (h1-valid "Linter ok")
                              (re-find #"linting took \d*ms, errors: \d*, warnings: \d*" (str out))
                              (do (h1-error "Linter found errors \n") (normalln err out))
                              :else
                              (do (errorln "Failed during linting.") (normalln out) (normalln)))
                            (print-writter s)
                            (zero? exit))))]
    linter-status))

(defn synthesis
  "Prints a synthesis line for `status`."
  [status]
  (cond
    (nil? status) nil
    (true? status) (h2-valid! "Linter ok")
    (false? status) (h2-error! "Linter has failed")))

(comment
  (require '[automaton-build.os.edn-utils :as build-edn])
  (lint (build-edn/read-edn "deps.edn") false)
  ;
)

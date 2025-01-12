(ns automaton-build.code.lint
  "Lint the application.

   Proxy to [clj-kondo](https://github.com/clj-kondo/clj-kondo?tab=readme-ov-file#usage) ")

(defn lint-cmd
  "Lint command in `paths`. If `debug?` is set, that informations are displayed."
  [debug? paths]
  (when-not (empty? paths)
    (-> (concat ["clj-kondo"] ;; Project still too small : "--parallel"
                (when debug? ["--debug"])
                ["--lint"]
                paths)
        vec)))

(comment
  (defn lint-project
    [dir]
    (let [dir (:app-dir deps-file-desc)
          linter-status
          (let [paths-to-lint (->> deps-file-desc
                                   :edn
                                   build-deps/get-src
                                   (keep build-file/is-existing-dir?))]
            (if (empty? paths-to-lint)
              (do (normalln "Nothing to lint") nil)
              (let [s (build-writter)
                    lint-cmd (build-code-lint/lint-cmd verbose? paths-to-lint)
                    {:keys [out err exit]} (binding [*out* s]
                                             (blocking-cmd lint-cmd dir "" verbose?))]
                (cond
                  (zero? exit) (h1-valid "Linter ok")
                  (re-find #"linting took \d*ms, errors: \d*, warnings: \d*" (str out))
                  (do (h1-error "Linter found errors \n") (normalln err out))
                  :else (do (errorln "Failed during linting") (normalln out) (normalln)))
                (print-writter s)
                (zero? exit))))]
      linter-status)))

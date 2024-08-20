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

(ns automaton-build.code.lint "Lint the application.")

(defn lint-cmd
  "Lint command in `paths`. If `debug?` is set, that informations are displayed."
  [debug? paths]
  (when-not (empty? paths)
    (-> (concat ["clj-kondo" "--lint"] paths (when debug? ["--debug"]))
        vec)))

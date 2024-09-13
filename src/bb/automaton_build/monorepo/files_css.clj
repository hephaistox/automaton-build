(ns automaton-build.monorepo.files-css)

(defn generate-custom-css
  [css-apps]
  (->> css-apps
       (map #(get-in % [:custom-css :raw-content]))
       (apply str)))

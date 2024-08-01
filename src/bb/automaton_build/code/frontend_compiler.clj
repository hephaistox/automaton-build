(ns automaton-build.code.frontend-compiler
  "Front end compiler toolings. Use shadow on npx.")

(defn fe-watch-cmd
  "Watch modification on frontend code for aliases `shadow-cljs-aliases`."
  [shadow-cljs-aliases]
  (-> ["npx" "shadow-cljs" "watch"]
      (concat shadow-cljs-aliases)
      vec))

(defn install-cmd
  "Install components setup in `package.json`."
  []
  ["npm install"])

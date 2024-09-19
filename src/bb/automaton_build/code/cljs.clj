(ns automaton-build.code.cljs
  "cljs compiler toolings. Use shadow on npx.

  Proxy to [npx](https://shadow-cljs.github.io/docs/UsersGuide.html#_command_line)")

(defn cljs-watch-cmd
  "Watch modification on frontend code for aliases `shadow-cljs-aliases`."
  [shadow-cljs-aliases]
  (-> ["npx" "shadow-cljs" "watch"]
      (concat shadow-cljs-aliases)
      vec))

(defn install-cmd "Install components setup in `package.json`." [] ["npm install"])

(defn cljs-compile-cmd
  "Command to compile the `builds` (vector of strings)."
  [builds]
  (reduce conj ["npx" "shadow-cljs" "compile"] builds))

(defn cljs-compile-release-cmd
  "Command to compile the `builds` (vector of strings)."
  [build]
  ["npx" "shadow-cljs" "release" build])

(defn karma-test-cmd
  "Returns a command to launch karma test."
  []
  ["npx" "karma" "start" "--single-run"])

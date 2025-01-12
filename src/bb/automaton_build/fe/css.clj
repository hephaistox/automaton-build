(ns automaton-build.fe.css
  "Load the file css.

  Proxy to [tailwindcss](https://tailwindcss.com/docs/installation)."
  (:require
   [automaton-build.os.file :as build-file]
   [clojure.string          :as str]))

(defn tailwind-compile-cmd
  "Returns the command to compile tailwind"
  [css-file compiled-dir]
  ["npx" "tailwindcss" "-i" css-file "-o" compiled-dir])

(defn tailwind-watch-cmd
  "Returns the command to watch tailwind modifications"
  [css-file compiled-dir]
  (conj (tailwind-compile-cmd css-file compiled-dir) "--watch"))

(defn tailwind-release-cmd
  "Returns the command to bulid tailwind in production"
  [css-file compiled-dir]
  (conj (tailwind-compile-cmd css-file compiled-dir) "--minify"))

(defn save-css [filename content] (build-file/write-file filename content))

(defn combine-css-files
  [& css-files]
  (let [combined-tmp-file (build-file/create-temp-file "combined.css")
        files-content (str/join "\n"
                                (map (fn [css-filename]
                                       (let [res (build-file/read-file css-filename)]
                                         (when-not (:invalid? res) (:raw-content res))))
                                     css-files))]
    (save-css combined-tmp-file files-content)
    combined-tmp-file))

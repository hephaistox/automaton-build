(ns automaton-build.fe.css
  "Load the file css.

  Proxy to [tailwindcss](https://tailwindcss.com/docs/installation)."
  (:require
   [automaton-build.os.file :as build-file]))

(def main-css "main.css")

(def custom-css "custom.css")

(defn tailwind-compile-cmd
  [css-file compiled-dir]
  ["npx" "tailwindcss" "-i" css-file "-o" compiled-dir])

(defn tailwind-watch-cmd
  [css-file compiled-dir]
  (conj (tailwind-compile-cmd css-file compiled-dir) "--watch"))

(defn tailwind-release-cmd
  [css-file compiled-dir]
  (conj (tailwind-compile-cmd css-file compiled-dir) "--minify"))

(defn save-css
  [filename content]
  (build-file/write-file
   filename
   (cons "/* This file is automatically updated by `automaton-build.fe.css` */"
         content)))

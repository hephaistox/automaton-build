(ns automaton-build.project.tailwind-config
  (:require
   [automaton-build.os.filename :as build-filename]
   [automaton-build.os.js-file  :as build-js-file]))

(def tailwind-config-js "tailwind.config.js")

(defn file->tailwind-require
  "Turns `file` from `dir` into tailwind require"
  [dir file]
  (build-js-file/js-require (str dir file)))

(defn dir->tailwind-content-path
  "Turn directory into tailwind content"
  [dir]
  (str "'" dir "src/**/*.{html,js,clj,cljs,cljc}'"))

(defn tailwind-files->tailwind-requires
  "Turns `files-paths` into tailwind requires relative to `dir`"
  [dir files-paths]
  (let [app-tailwind-requires (mapv (partial file->tailwind-require dir) files-paths)]
    (when-not (or (nil? files-paths) (empty? files-paths)) app-tailwind-requires)))

(defn load-tailwind-config
  "Read the tailwind-config from `dir`
  Params:
  * `dir` the directory of the application
  Returns the content as data structure"
  [dir]
  (let [package-filepath (build-filename/create-file-path dir tailwind-config-js)]
    (build-js-file/load-js-config package-filepath)))

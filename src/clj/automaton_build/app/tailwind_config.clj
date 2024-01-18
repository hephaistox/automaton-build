(ns automaton-build.app.tailwind-config
  (:require
   [automaton-build.os.files :as build-files]
   [automaton-build.os.js-config :as js-config]))

(def tailwind-config-js "tailwind.config.js")

(defn file->tailwind-require
  "Turns `file` from `dir` into tailwind require"
  [dir file]
  (js-config/js-require (str dir file)))

(defn dir->tailwind-content-path
  "Turn directory into tailwind content"
  [dir]
  (str "'" dir "src/**/*.{html,js,clj,cljs,cljc}'"))

(defn tailwind-files->tailwind-requires
  "Turns `files-paths` into tailwind requires relative to `dir`"
  [dir files-paths]
  (let [app-tailwind-requires (mapv (partial file->tailwind-require dir)
                                    files-paths)]
    (when-not (or (nil? files-paths) (empty? files-paths))
      app-tailwind-requires)))

(defn load-tailwind-config
  "Read the tailwind-config from `dir`
  Params:
  * `dir` the directory of the application
  Returns the content as data structure"
  [dir]
  (let [package-filepath (build-files/create-file-path dir tailwind-config-js)]
    (js-config/load-js-config package-filepath)))

(defn write-tailwind-config
  "Saves `content` in the `dir`
  Params:
  * `dir`
  * `content`"
  [dir tailwind-config]
  (let
    [tailwind-config
     (apply
      str
      ["/* This file is automatically updated by `automaton-build.app.tailwind-config` */"
       tailwind-config])
     package-filepath (build-files/create-file-path dir tailwind-config-js)]
    (js-config/write-js-config package-filepath tailwind-config)))

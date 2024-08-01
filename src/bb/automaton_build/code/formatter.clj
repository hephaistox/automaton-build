(ns automaton-build.code.formatter
  "Format code to apply rules in `zprintrc`.

  Proxy to [zprint](https://github.com/kkinnear/zprint)"
  (:require
   [automaton-build.os.file :as build-file]))

(def ^:private use-local-zprint-config-parameter
  "As described in the documentation below, by default `zprint` uses the local configuration.

  If this parameter is set locally, the project configuration will bee used.
  [zprint documentation](https://github.com/kkinnear/zprint/blob/main/doc/using/project.md#use-zprint-with-different-formatting-for-different-projects)"
  #":search-config\?\s*true")

(def ^:private zprint-file "~/.zprintrc")

(defn is-zprint-using-project-setup?
  []
  (some->> (build-file/read-file zprint-file)
           (re-find use-local-zprint-config-parameter)))

(defn format-file-cmd
  "Format the `filename` clojure file with zprint."
  [filename]
  ["zprint" "-w" filename])

(defn format-clj-cmd
  "Command formatting all clj files in the directory and subdirectories where it is executed."
  []
  ["fd" "-e" "clj" "-e" "cljc" "-e" "cljs" "-e" "edn" "-x" "zprint" "-w"])

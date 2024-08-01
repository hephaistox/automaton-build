(ns automaton-build.os.filename
  "Manipulate file names (is not influenced at all by your local configuration)."
  (:require
   [babashka.fs    :as fs]
   [clojure.string :as str]))

(def directory-separator
  "Symbol to separate directories.
  Is usually `/` on linux based OS And `\\` on windows based ones"
  fs/file-separator)

(defn remove-trailing-separator
  "If exists, remove the trailing separator in a path, remove unwanted spaces either"
  [path]
  (let [path (str/trim path)]
    (if (= (str directory-separator) (str (last path)))
      (->> (dec (count path))
           (subs path 0)
           remove-trailing-separator)
      path)))

(defn absolutize
  "Returns the absolute path of `relative-path` (file or dir)."
  [relative-path]
  (when relative-path (str (fs/absolutize relative-path))))

(defn match-extension?
  "Returns true if the `filename` match the at least one of the `extensions`."
  [filename & extensions]
  (when-not (str/blank? filename)
    (some (fn [extension] (str/ends-with? filename extension)) extensions)))

(defn change-extension
  "Turns `filename` extension into `new-extension`."
  [file-name new-extension]
  (str (fs/strip-ext file-name) new-extension))

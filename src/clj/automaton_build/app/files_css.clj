(ns automaton-build.app.files-css
  "Code for manipulation of css files"
  (:require
   [automaton-build.os.files :as build-files]
   [clojure.string           :as str]))

(def main-css "main.css")

(def custom-css "custom.css")

(defn combine-css-files
  [& css-files]
  (let [combined-tmp-file (build-files/create-temp-file "combined.css")
        files-content (str/join "\n" (map #(slurp %) css-files))]
    (build-files/spit-file combined-tmp-file
                           files-content
                           nil
                           (fn [_ _ _] false))
    combined-tmp-file))


(defn- new-load-css-file
  "Returns string from reading app css file."
  [app-dir filename]
  (-> (build-files/create-file-path app-dir filename)
      build-files/read-file))

(defn- write-css-file
  "Saves css content to a file"
  [path content]
  (build-files/spit-file
   path
   content
   "/* This file is automatically updated by `automaton-build.app.files-css` */"))

(defn write-main-css-file
  "Create main css file for monorepo"
  [app-dir main-css-path save-path]
  (let [main-css-file (new-load-css-file app-dir main-css-path)]
    (write-css-file (build-files/create-file-path save-path main-css)
                    main-css-file)))

(defn write-custom-css-file
  "Create custom css file for monorepo from `css-files-paths` that are vectors where first element is a directory and second filename"
  [css-files-paths save-path]
  (let [css-files (map #(new-load-css-file (first %) (second %))
                       css-files-paths)
        custom-css-file (apply str css-files)]
    (write-css-file (build-files/create-file-path save-path custom-css)
                    custom-css-file)))

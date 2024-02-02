(ns automaton-build.app.shadow-cljs
  (:require
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files :as build-files]
   [automaton-build.utils.map :as build-utils-map]))

(def shadow-cljs-edn "shadow-cljs.edn")

(defn template-build
  [template [build-alias build-value]]
  {build-alias (assoc template
                      :asset-path (:asset-path build-value)
                      :modules (:modules build-value)
                      :output-dir (:output-dir build-value))})

(defn template-builds
  [build-template build-aliases]
  (map (partial template-build build-template) build-aliases))

(defn shadow-cljs-config
  [aliases]
  (reduce (fn [acc m] (update acc :builds merge m)) {:builds {}} aliases))

(defn get-template-build
  [template-shadow-cljs]
  (get-in template-shadow-cljs [:builds :app-template]))

(defn template-shadow-cljs-config
  [build-template build-aliases]
  (let [shadow-cljs-templated-builds (template-builds build-template
                                                      build-aliases)]
    (shadow-cljs-config shadow-cljs-templated-builds)))

(defn remove-template-module
  "Removes build template"
  [shadow-cljs]
  (update-in shadow-cljs [:builds] dissoc :app-template))

(defn merge-shadow-cljs-configs
  [& configs]
  (apply build-utils-map/deep-merge configs))

(defn create-shadow-cljs-from-template
  [template-shadow-cljs build-aliases]
  (let [build-alias-template (get-template-build template-shadow-cljs)
        apps-shadow-cljs (template-shadow-cljs-config build-alias-template
                                                      build-aliases)
        prepared-main-shadow-cljs (remove-template-module template-shadow-cljs)
        new-shadow-cljs (merge-shadow-cljs-configs prepared-main-shadow-cljs
                                                   apps-shadow-cljs)]
    new-shadow-cljs))

(defn get-shadow-filename
  "Get the deps-file of the application
  Params:
  * `dir` is where the application is stored"
  [dir]
  (build-files/create-file-path dir shadow-cljs-edn))

(defn write-shadow-cljs
  "Save `content` in the filename path
  Params:
  * `dir`
  * `content`"
  [dir content]
  (build-edn-utils/spit-edn
   (get-shadow-filename dir)
   content
   "This file is automatically updated by `automaton-build.app.shadow-cljs`"))

(defn load-shadow-cljs
  "Read the shadow-cljs of an app
  Params:
  * `dir` the directory of the application
  Returns the content as data structure"
  [dir]
  (let [shadow-filepath (build-files/create-file-path dir shadow-cljs-edn)]
    (when (build-files/is-existing-file? shadow-filepath)
      (build-edn-utils/read-edn shadow-filepath))))

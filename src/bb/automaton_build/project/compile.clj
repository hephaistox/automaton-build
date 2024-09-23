(ns automaton-build.project.compile
  (:require
   [automaton-build.code.artifacts :as build-code-artifacts]
   [automaton-build.code.cljs      :as build-cljs]
   [automaton-build.echo.headers   :refer [build-writter]]
   [automaton-build.fe.css         :as build-fe-css]
   [automaton-build.os.cmds        :as build-commands]
   [automaton-build.os.file        :as build-file]
   [automaton-build.os.filename    :as build-filename]))

(defn shadow-cljs
  [app-dir deploy-alias]
  (let [res (-> [[(build-cljs/install-cmd)]]
                (concat [[(build-cljs/cljs-compile-release-cmd deploy-alias)]])
                (build-commands/force-dirs app-dir)
                build-commands/chain-cmds
                build-commands/first-failing)]
    (if (build-commands/success res)
      {:status :success}
      {:status :failed
       :res res})))

(defn css
  [app-dir input-css-files output-css-path]
  (let [input-css-file (apply build-fe-css/combine-css-files input-css-files)
        res (-> [[(build-cljs/install-cmd)]]
                (concat [[(build-fe-css/tailwind-release-cmd input-css-file output-css-path)]])
                (build-commands/force-dirs app-dir)
                build-commands/chain-cmds
                build-commands/first-failing)]
    (if (build-commands/success res)
      {:status :success}
      {:status :failed
       :res res})))


(defn compile-jar
  "Compile code to jar or uber-jar based on `jar-type`."
  [class-dir app-paths target-jar-path project-dir]
  (try (let [app-paths (mapv #(build-filename/absolutize (build-filename/create-dir-path project-dir
                                                                                         %))
                             app-paths)
             s# (build-writter)
             ;; This require is needed to go around this issue: https://github.com/babashka/pods/issues/72
             _ (require 'clojure.tools.deps.util.io :reload)]
         (->> app-paths
              build-file/file-rich-list
              (mapv (fn [path]
                      (-> path
                          (assoc :options
                                 {:replace-existing true
                                  :copy-attributes true})
                          (assoc :target-dir-path class-dir))))
              build-file/actual-copy)
         (binding [*err* s#]
           (build-code-artifacts/set-project-root! (build-filename/absolutize project-dir)))
         (binding [*err* s#]
           (build-code-artifacts/jar {:class-dir class-dir
                                      :jar-file target-jar-path}))
         {:status :success
          :jar-path target-jar-path
          :msg (str s#)})
       (catch Exception e
         {:status :failed
          :exception e})))

(defn compile-uber-jar
  "Compile code to jar or uber-jar based on `jar-type`."
  [class-dir app-paths target-jar-path project-dir jar-main java-opts]
  (try
    (let [app-paths (mapv #(build-filename/absolutize (build-filename/create-dir-path project-dir
                                                                                      %))
                          app-paths)
          s (build-writter)
          ;; This require is needed to go around this issue: https://github.com/babashka/pods/issues/72
          _ (require 'clojure.tools.deps.util.io :reload)]
      (->> app-paths
           build-file/file-rich-list
           (mapv (fn [path]
                   (let [copy-action (-> path
                                         (assoc :options
                                                {:replace-existing true
                                                 :copy-attributes true})
                                         (assoc :target-dir-path class-dir))]
                     copy-action)))
           build-file/actual-copy)
      (binding [*out* s
                *err* s]
        (build-code-artifacts/set-project-root! (build-filename/absolutize project-dir))
        (let [basis (build-code-artifacts/create-basis)]
          (build-code-artifacts/compile-clj {:basis basis
                                             :class-dir class-dir
                                             :java-opts java-opts})
          (build-code-artifacts/uber {:class-dir class-dir
                                      :uber-file target-jar-path
                                      :basis basis
                                      :main jar-main})))
      {:status :success
       :msg (str s)
       :jar-path target-jar-path})
    (catch Exception e
      {:status :failed
       :exception e})))


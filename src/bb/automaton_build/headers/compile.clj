(ns automaton-build.headers.compile
  (:require
   [automaton-build.code.cljs   :as build-cljs]
   [automaton-build.fe.css      :as build-fe-css]
   [automaton-build.headers.cmd :as build-headers-cmd]))

(defn shadow-cljs
  [app-dir deploy-alias]
  (let [process (-> (build-cljs/install-cmd)
                    (build-headers-cmd/print-on-error app-dir 1 100 100))
        ;; (concat [[(build-cljs/cljs-compile-release-cmd deploy-alias)]])
        ;;TODO Add a fail to nil function, so they'll be threaded with some?
       ]
    process))

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

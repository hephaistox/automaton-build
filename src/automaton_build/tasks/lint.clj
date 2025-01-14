(ns automaton-build.tasks.lint
  (:require
   [automaton-build.echo        :as build-echo]
   [automaton-build.os.cli-opts :as build-cli-opts]
   [automaton-build.os.cmd      :as build-cmd]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; *** Private
;; ********************************************************************************

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn lint-cmd
  "Lint command in `paths`. If `debug?` is set, that informations are displayed."
  [debug? paths]
  (when-not (empty? paths)
    (-> (concat ["clj-kondo"] ;; Project still too small : "--parallel"
                (when debug? ["--debug"])
                ["--lint"]
                paths)
        vec)))

(defn lint
  "Lint `paths`

  ff `debug?` is `true`,  linter provides detailed informations"
  [{:keys [normalln errorln]
    :as _printers}
   debug?
   app-dir]
  (let [paths ["src"]]
    (-> (lint-cmd debug? paths)
        (build-cmd/print-on-error app-dir normalln errorln 10 100 100))))

(defn lint-one-line-headers
  [debug? app-dir]
  (let [{:keys [h1 h1-valid! h1-error! h2 h2-error! normalln errorln uri-str]}
        (:one-liner-headers build-echo/printers)]
    (lint {:title h1
           :title-valid h1-valid!
           :title-error h1-error!
           :subtitle h2
           :subtitle-error h2-error!
           :normalln normalln
           :errorln errorln
           :uri-str uri-str}
          debug?
          app-dir)))

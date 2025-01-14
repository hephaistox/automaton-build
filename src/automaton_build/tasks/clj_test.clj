(ns automaton-build.tasks.clj-test
  "Test the project with clj compiler"
  (:require
   [automaton-build.echo          :as build-echo]
   [automaton-build.os.cli-opts   :as build-cli-opts]
   [automaton-build.os.cmd        :as build-os-cmd]
   [automaton-build.os.edn-utils  :as build-edn-utils]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.filename   :refer [absolutize]]
   [automaton-build.tasks.lint    :as build-lint]
   [clojure.set                   :as set]
   [clojure.string                :as str]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options [["-l" "--lint" "Linter"]])
      build-cli-opts/parse-cli-args
      (update :arguments #(mapv keyword %))))

;;TODO Extract argument as a list
(defn validate-arguments
  [test-definitions current-task]
  (->> {:arguments "ALIASES"
        :arguments-desc
        (str "ALIASES should be one of: " (str/join ", " test-definitions) ", or all")
        :parse-fn keyword
        :valid-fn #(do (println "Is " %)
                       (println "Should be " (set test-definitions) ", or all")
                       (or (= % "all") (set/intersection (set test-definitions) (set %))))}
       (build-cli-opts/enter-with-arguments cli-opts current-task)))

(def verbose (get-in cli-opts [:options :verbose]))

; ********************************************************************************
; *** Private
; ********************************************************************************

(defn- test-cmd
  [test-runner-alias test-definitions]
  (->> test-definitions
       (mapv (fn [alias]
               {:cmd ["clojure" (str "-M:" test-runner-alias alias)]
                :alias alias}))))

(defn- alias-in-deps-edn
  [{:keys [errorln uri-str]
    :as _printers}]
  (let [{:keys [invalid? edn dir]} (build-edn-utils/read-edn "deps.edn")]
    (if invalid?
      (do (errorln "`deps.edn` file is invalid in dir "
                   (-> dir
                       absolutize
                       uri-str))
          (System/exit -1))
      (-> edn
          :aliases
          keys))))

(defn- validate-definitions
  [test-definitions alias-in-deps-edn]
  (group-by (fn [alias]
              (if (contains? (set alias-in-deps-edn) alias) :alias-exist :alias-doesnt-exist))
            test-definitions))

(defn- limit-selection
  "Limit test definitions to the one selected by the user"
  [test-definitions selected]
  (let [selected (if (contains? (set selected) :all) (set test-definitions) (set selected))]
    {:selected (set/intersection selected (set test-definitions))
     :test-definitions test-definitions
     :non-existing (set/difference selected (set test-definitions))
     :filtered (set (filterv #(contains? selected %) test-definitions))}))

; ********************************************************************************
; *** Task code
; ********************************************************************************

(defn run
  "Run tests"
  [{:keys [title title-valid title-error subtitle subtitle-error normalln errorln]
    :as printers}
   test-runner-alias
   test-definitions
   current-task]
  (validate-arguments test-definitions current-task)
  (let [app-dir ""
        alias-in-deps-edn (alias-in-deps-edn printers)
        {:keys [alias-doesnt-exist alias-exist]} (->> alias-in-deps-edn
                                                      (validate-definitions test-definitions))
        {:keys [non-existing selected filtered]} (->> (mapv keyword (:arguments cli-opts))
                                                      (limit-selection alias-exist))]
    ;;TODO Check that args dfferent use cases
    ;;TODO Extract test as linter is extracted
    (when-not (empty? alias-doesnt-exist)
      (errorln "These tests are skipped as no selected alias exist in `deps.edn`: "
               (str/join "," alias-doesnt-exist)
               " (existing are "
               (str/join "," test-definitions)
               ")"))
    (when-not (empty? non-existing)
      (errorln "The following dep test aliases are unkonwn: " non-existing))
    (if (empty? selected)
      (do (title-error "Nothing to test, pick one of " test-definitions ", or all") -1)
      (do (title "Tested environments:" selected)
          (when (:linter cli-opts) (build-lint/lint printers false app-dir))
          (let [exit-codes
                (->> filtered
                     (test-cmd test-runner-alias)
                     (keep
                      (fn [{:keys [cmd alias]}]
                        (subtitle "Tests" alias)
                        (let [{:keys [status]}
                              (build-os-cmd/print-on-error cmd app-dir normalln errorln 10 100 100)]
                          (when-not (= :success status)
                            (subtitle-error "Tests" alias "have failed"))
                          status))))]
            (if (every? #(= :success %) exit-codes)
              (do (title-valid "Tests passed") build-exit-codes/ok)
              (do (title-error "Tests have failed") build-exit-codes/invalid-state)))))))

    ;;TODO Extract printer
(defn run-one-liner-headers
  [test-runner-alias test-definitions current-task]
  (let [{:keys [h1 h1-valid! h1-error! h2 h2-error! normalln errorln uri-str]}
        (:one-liner-headers build-echo/printers)]
    (run {:title h1
          :title-valid h1-valid!
          :title-error h1-error!
          :subtitle h2
          :subtitle-error h2-error!
          :normalln normalln
          :errorln errorln
          :uri-str uri-str}
         test-runner-alias
         test-definitions
         current-task)))

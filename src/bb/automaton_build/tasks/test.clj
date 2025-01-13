(ns automaton-build.tasks.test
  "Test the project"
  (:require
   [automaton-build.echo          :as build-echo]
   [automaton-build.os.cli-opts   :as build-cli-opts]
   [automaton-build.os.cmd        :as build-os-cmd]
   [automaton-build.os.edn-utils  :as build-edn-utils]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.filename   :refer [absolutize]]
   [clojure.set                   :as set]
   [clojure.string                :as str]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

; ********************************************************************************
; *** Task code
; ********************************************************************************

(defn- test-cmd
  [test-runner-alias test-definitions]
  (->> test-definitions
       (mapv (fn [{:keys [alias]}]
               {:cmd ["clojure" (str "-M:" test-runner-alias alias)]
                :alias alias}))))

(defn alias-in-deps-edn
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

(defn validate-definitions
  [test-definitions alias-in-deps-edn]
  (let [alias-in-deps-edn (set alias-in-deps-edn)]
    (group-by (fn [{:keys [alias]}]
                (if (contains? alias-in-deps-edn alias) :alias-exist :alias-doesnt-exist))
              test-definitions)))

(defn select
  "Limit test definitions to the one selected by the user"
  [test-definitions selected]
  (let [selected
        (if (contains? (set selected) :all) (set (mapv :alias test-definitions)) (set selected))]
    {:selected (set/intersection selected (set (mapv :alias test-definitions)))
     :test-definitions test-definitions
     :non-existing (set/difference selected (set (mapv :alias test-definitions)))
     :filtered (set (filterv #(contains? selected (:alias %)) test-definitions))}))

(defn run
  "Run tests"
  [{:keys [title title-valid title-error subtitle subtitle-error normalln errorln]
    :as printers}
   test-runner-alias
   test-definitions
   current-task]
  (build-cli-opts/enter cli-opts current-task)
  (let [alias-in-deps-edn (alias-in-deps-edn printers)
        {:keys [alias-doesnt-exist alias-exist]} (->> alias-in-deps-edn
                                                      (validate-definitions test-definitions))
        {:keys [non-existing selected filtered]} (->> (mapv keyword (:arguments cli-opts))
                                                      (select alias-exist))]
    (when-not (empty? alias-doesnt-exist)
      (errorln "These tests are skipped as alias doesnt exist in `deps.edn`: "
               (str/join "," (map :alias alias-doesnt-exist))))
    (when-not (empty? non-existing) (errorln "The following test are unkonwn: " non-existing))
    (if (empty? selected)
      (do (title-error "Nothing to test, pick one of " (mapv :alias test-definitions) ", or all")
          -1)
      (do (title "Tested environments:" selected)
          (let [exit-codes
                (->> filtered
                     (test-cmd test-runner-alias)
                     (keep
                      (fn [{:keys [cmd alias]}]
                        (subtitle "Tests" alias)
                        (let [{:keys [status]}
                              (build-os-cmd/print-on-error cmd "." normalln errorln 10 100 100)]
                          (when-not (= :success status)
                            (subtitle-error "Tests" alias "have failed"))
                          status))))]
            (if (every? #(= :success %) exit-codes)
              (do (title-valid "Tests passed") build-exit-codes/ok)
              (do (title-error "Tests have failed") build-exit-codes/invalid-state)))))))

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

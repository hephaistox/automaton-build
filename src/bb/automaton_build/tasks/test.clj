(ns automaton-build.tasks.test
  "Test the project"
  (:require
   [automaton-build.echo.headers            :refer [errorln h1 h1-error! h1-valid!]]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.edn-utils            :as build-edn-utils]
   [automaton-build.os.filename             :refer [absolutize]]
   [automaton-build.tasks.impl.headers.cmds :as headers-cmds]
   [clojure.set                             :as set]
   [clojure.string                          :as str]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(def ^:private cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def verbose (get-in cli-opts [:options :verbose]))

(def help (get-in cli-opts [:options :help]))

; ********************************************************************************
; *** Task code
; ********************************************************************************

(defn test-command
  [test-runner-alias test-definitions]
  (->> test-definitions
       (mapv (fn [{:keys [alias]}]
               {:cmd ["clojure" (str "-M:" test-runner-alias alias)]
                :alias alias}))))

(defn alias-in-deps-edn
  []
  (let [{:keys [invalid? edn dir]} (build-edn-utils/read-edn "deps.edn")]
    (if invalid?
      (do (errorln "`deps.edn` file is invalid in dir " (absolutize dir)) (System/exit -1))
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
  [test-runner-alias test-definitions]
  (if help
    (println
     (build-cli-opts/print-usage-with-arguments cli-opts "test" "test" "[unit|development|la]"))
    (let [alias-in-deps-edn (alias-in-deps-edn)
          {:keys [alias-doesnt-exist alias-exist]} (validate-definitions test-definitions
                                                                         alias-in-deps-edn)
          arguments (mapv keyword (:arguments cli-opts))
          {:keys [non-existing selected filtered]} (select alias-exist arguments)]
      (when-not (empty? alias-doesnt-exist)
        (errorln "These tests are skipped as alias doesnt exist in `deps.edn`: "
                 (str/join "," (map :alias alias-doesnt-exist))))
      (when-not (empty? non-existing) (errorln "The following test are unkonwn: " non-existing))
      (if (empty? selected)
        (do (h1-error! "Nothing to test, pick one of " (mapv :alias test-definitions) ", or all")
            -1)
        (do (h1 "Tested environments:" selected)
            (let [exit-codes (->> (test-command test-runner-alias filtered)
                                  (keep (fn [{:keys [cmd alias]}]
                                          (let [{:keys [exit]
                                                 :as res}
                                                (headers-cmds/blocking-cmd
                                                 cmd
                                                 "."
                                                 (str "Tests" alias "have failed")
                                                 verbose)]
                                            (when (headers-cmds/success res)
                                              (h1-valid! "Tests" alias "passed"))
                                            exit))))]
              (if (every? zero? exit-codes)
                0
                (->> exit-codes
                     (remove zero?)
                     first))))))))

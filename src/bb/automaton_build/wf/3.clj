(ns automaton-build.wf.3
  "Quick code check."
  (:require
   [automaton-build.app-data.deps :as build-deps]
   [automaton-build.echo.headers  :as build-echo-headers]
   [automaton-build.os.cli-opts   :as build-cli-opts]
   [automaton-build.wf.common     :as build-wf-common]
   [automaton-build.wf.tests      :as build-wf-tests]
   [clojure.string                :as str]))

(def cli-opts
  (-> [build-wf-tests/lint-opts
       build-wf-tests/reports-opts
       build-wf-tests/commit-message]
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              build-cli-opts/log-options)
      build-cli-opts/parse-cli))

(def registry
  [{:to-skip? #(get-in % [:options :lint])
    :check-name "(clj[s] code linter"
    :fn-to-execute build-wf-tests/lint}
   {:to-skip? #(empty? (get-in % [:options :commit-message]))
    :check-name "Commit"
    :fn-to-execute build-wf-tests/commit}
   {:to-skip? #(get-in % [:options :reports])
    :check-name "Reports"
    :fn-to-execute build-wf-tests/reports}])

(defn run
  "Execute all tests and reports."
  []
  (build-echo-headers/normalln "Quick code check.")
  (let [deps (build-deps/deps-edn build-wf-common/app-dir)]
    (doseq [{:keys [to-skip? fn-to-execute check-name]} registry]
      (build-echo-headers/h1 "Check" check-name)
      (when (to-skip? cli-opts)
        (let [str-writer (java.io.StringWriter.)
              {:keys [message details status]} (binding [*out* str-writer]
                                                 (fn-to-execute deps cli-opts))]
          (if (= :ok status)
            (build-echo-headers/h1-valid "Check" check-name)
            (do (build-echo-headers/h1-error "Failed check"
                                             (str check-name ":"))
                (build-echo-headers/normalln message)))
          (let [s (.toString str-writer)] (when-not (str/blank? s) (println s)))
          (when details (build-echo-headers/normalln details)))))))

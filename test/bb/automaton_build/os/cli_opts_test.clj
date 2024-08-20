(ns automaton-build.os.cli-opts-test
  (:require
   [automaton-build.os.cli-opts :as sut]
   [clojure.test                :refer [deftest is]]))

(deftest usage-test
  (is (= {:options {}
          :arguments []
          :summary "  -h, --help  Print usage."
          :errors nil}
         (sut/parse-cli [] sut/help-options))
      "Only one defaulted option is ok.")
  (is (= {:options {:help true}
          :arguments []
          :summary "  -h, --help  Print usage."
          :errors nil}
         (sut/parse-cli ["-h"] sut/help-options)))
  (is (= {:options {}
          :arguments []
          :summary "  -h, --help  Print usage."
          :errors ["Unknown option: \"-e\""]}
         (sut/parse-cli ["-e"] sut/help-options))
      "An unknown option is detected."))

(deftest inverse-test
  (is (= {:options {:repl true
                    :mermaid false
                    :foo false}}
         (sut/inverse {:options {:repl true
                                 :mermaid false
                                 :foo false}}
                      [:repl :mermaid]))
      "If inverse is not set, no value change.")
  (is (= {:options {:repl true
                    :inverse true
                    :mermaid true
                    :foo false}}
         (sut/inverse {:options {:repl false
                                 :inverse true
                                 :mermaid false
                                 :foo false}}
                      [:repl :mermaid]))
      "If inverse is set, no value change."))

(deftest errors-test
  (is (= ["Unknown option: \"-e\""]
         (-> (sut/parse-cli ["-e"] sut/help-options)
             sut/errors))
      "Errors are detected."))

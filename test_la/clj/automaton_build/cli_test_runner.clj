(ns automaton-build.cli-test-runner
  "Test all the cli that they returns the expected exit code, as setup in the registry"
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.commands   :as build-cmds]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn- run-cmd
  "Run a command
  Params:
  * `cmd` command to execute
  * `expanded-cmd` command ready to display
  * `expected-exit-code` exit code that the cmd should return
  * `cli-args` command line arguments
  * `process-opts` options to pass to process creation"
  [cmd expanded-cmd expected-exit-code cli-args process-opts]
  (let [cmd-with-args (concat cmd cli-args [process-opts])
        [[exit-code _]] (build-cmds/execute-and-trace-return-exit-codes cmd-with-args)]
    (if (= expected-exit-code exit-code)
      [true #(build-log/info-format "Test `%s` successfully passed" expanded-cmd)]
      [false
       #(build-log/error-format "Test `%s` expects `%s` and found `%s`"
                                expanded-cmd
                                expected-exit-code
                                exit-code)])))

(defn- test-cli-cmd
  "Run the command, print the message
  Params:
  * `cli-args` command line arguments
  * `map-cmd` map of the command to execute"
  [cli-args
   [_task-name
    {:keys [la-test]
     :as _map-cmd}]]
  (let [{:keys [skip? process-opts cmd expected-exit-code]
         :or {expected-exit-code build-exit-codes/ok}}
        la-test
        expanded-cmd (build-cmds/expand-cmd cmd)]
    (if (or (empty? la-test) skip?)
      [true #(build-log/debug-format "Skip `%s` " expanded-cmd)]
      (do (build-log/debug-format "Test cmd `%s`:" expanded-cmd)
          (run-cmd cmd expanded-cmd expected-exit-code cli-args process-opts)))))

(defn- exec-and-return
  "In a command result, execute the display-return-fn and return the value"
  [[passed? display-return-fn]]
  (display-return-fn)
  [passed? display-return-fn])

(defn cli-test
  "Test cli commands,
  Will execute tasks from the registry passed

  Design decision:
  * the mechanism to decide which tasks are considered in the `bb.edn` should not be reproduced here, relying on the bb.edn tasks name is enough

  Params:
  * `task-registry`
  * `cli-args` arguments of the cli - useful to keep that setup in the called bb tasks"
  [task-registry cli-args]
  (let [results (->> task-registry
                     (filter (fn [[_ task-map]] (:la-test task-map)))
                     (mapv (comp exec-and-return (partial test-cli-cmd cli-args))))]
    (build-log/info "Summary")
    (doseq [[_ returned-value-fn] results] (returned-value-fn))
    (if (every? first results)
      (do (build-log/info "All tests passed") build-exit-codes/ok)
      (do (build-log/error "Errors found") build-exit-codes/catch-all))))

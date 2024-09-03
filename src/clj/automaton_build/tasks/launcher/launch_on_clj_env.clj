(ns automaton-build.tasks.launcher.launch-on-clj-env
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.commands   :as build-cmds]
   [automaton-build.os.edn-utils  :as build-edn-utils]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn switch-to-clj
  "Run the `task-fn` on a clojure environment"
  [{:keys [task-fn]
    :as task-map}
   app-data
   task-cli-opts
   cli-args]
  (let [clj-input-tmp-file (build-edn-utils/create-tmp-edn "to-clj-x.edn")]
    (build-edn-utils/spit-edn clj-input-tmp-file
                              {:app-data app-data
                               :cli-args cli-args
                               :task-cli-opts task-cli-opts
                               :task-map task-map
                               :task-fn task-fn})
    (build-log/trace-format "Data passed to file `%s` to launch clojure" clj-input-tmp-file)
    (let [exit-code (ffirst
                     (build-cmds/execute-and-trace-return-exit-codes
                      (vector
                       "clojure"
                       "-X:build:bb-deps"
                       (str 'automaton-build.tasks.launcher.launch-on-clj-entry-point/entry-point)
                       :clj-input-tmp-file (format "\"%s\"" clj-input-tmp-file))))]
      (if (int? exit-code)
        exit-code
        (do (build-log/trace "The clj command has failed, so the exit code is passed to the bb")
            build-exit-codes/catch-all)))))

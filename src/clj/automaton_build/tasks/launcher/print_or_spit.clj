(ns automaton-build.tasks.launcher.print-or-spit
  "Print or spit an exception

  Don't use build_files, it would create a cycle dependency"
  (:require
   [babashka.fs :as fs]))

(defn exception
  "Print or spit the exception depending on the `spit?` parameter

  This version is necessary for errors during task-helper, as using something else would create cycle dependencies
  Params:
  * `spit?` boolean
  * `e` exception"
  [spit? e]
  (if spit?
    (let [file (fs/create-temp-file {:suffix ".edn"})]
      (when-let [msg (ex-message e)] (println msg))
      (println (format "See details in `%s`" (.toString (.toAbsolutePath file))))
      (spit (fs/file file) (prn-str e)))
    (println e)))

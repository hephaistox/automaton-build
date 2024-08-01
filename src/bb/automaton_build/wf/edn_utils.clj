(ns automaton-build.wf.edn-utils
  "Edn access with echoing errors."
  (:require
   [automaton-build.echo.common     :as build-echo-common]
   [automaton-build.os.edn-utils-bb :as build-edn-utils-bb]))

(defn read-edn
  "Read the edn and tells if an error occcurs."
  [edn-filename]
  (try (build-edn-utils-bb/read-edn edn-filename)
       (catch Exception e
         (build-echo-common/exceptionln
          (ex-info (format "File `%s` is not an edn." edn-filename)
                   {:caused-by e
                    :file-name edn-filename}))
         nil)))

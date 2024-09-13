(ns automaton-build.tasks.clean
  (:require
   [automaton-build.code.vcs                :as build-vcs]
   [automaton-build.echo.headers            :refer [h1-error! h1-valid! normalln]]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.tasks.impl.headers.cmds :refer [simple-shell]]
   [clojure.pprint                          :as pp]))

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def verbose (get-in cli-opts [:options :verbose]))

(defn clean
  "Deletes the files which are given in the list.
  They could be regular files or directory, when so the whole subtreee will be removed"
  [file-list]
  (normalln (str "Starting removal of cache: \n" (with-out-str (pp/pprint file-list))))
  (try
    (let [removed-files (mapv (fn [file]
                                (let [res (build-file/delete-path file)]
                                  (when (not (or (= res file) (nil? res))) {file res})))
                              file-list)]
      (if (every? nil? removed-files)
        (h1-valid! "Cache cleaned")
        (h1-error! "Removal failed for: " (with-out-str (pp/pprint (remove nil?))) removed-files)))
    (catch Exception e (h1-error! "There was a problem while removing files" e))))

(defn clean-hard
  []
  (normalln "Starting a reset of repository state to a fresh clone from git server")
  (let [res (-> (build-vcs/clean-hard-cmd)
                simple-shell)]
    (if (= 0 (:exit res))
      (h1-valid! "Successfully cleaned the state")
      (h1-error! "Clean hard failed with: " res))))

(ns automaton-build.storage
  "Entry point to setup and start the storage - persistance of data.

  Design decision:
  * the installed version keep the version number in the path, so running a path is running the specified version for sure. In other words, renaming the path to remove the version may call inappropriate version
  * data persistance can leverage multiple technologies,
  * here is a first step for persistance, for general use,
  * specific needs will be addressed later on, with more specific technologies
  * the choice of technology should be as independant as possible, so a defprotocol is welcome to access their features"
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.commands   :as build-cmds]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files      :as build-files]
   [clojure.string                :as str]))

(defn- is-datomic-transactor?
  "Returns true if datomic pro transactor is already existing.

  Params:
  * `datomic-dir-setup` where datomic binary are extracted
  * `datomic-transactor-bin-path` the sub directory where the transactor binary is in the datomic dir"
  [datomic-dir-setup datomic-transactor-bin-path]
  (let [transactor-filename (build-files/create-dir-path datomic-dir-setup
                                                         datomic-transactor-bin-path)
        res (build-files/is-existing-file? transactor-filename)]
    (if res
      (build-log/debug-format "Transactor found in %s" transactor-filename)
      (build-log/debug-format "Transfactor not found in %s." transactor-filename))
    res))

(defn- datomic-dir-setup
  [datomic-root-dir datomic-dir-pattern datomic-ver]
  (build-files/create-dir-path (build-files/expand-home datomic-root-dir)
                               (format datomic-dir-pattern datomic-ver)))

(defn- datomic-dir
  [datomic-root-dir datomic-dir-pattern datomic-ver]
  (-> (datomic-dir-setup datomic-root-dir datomic-dir-pattern datomic-ver)
      build-files/expand-home))

(defn setup-datomic
  "Downloads datomic transactor and creates needed directories for it.
  As a result datomic transactor will be in the `datomic-dir`, named as datomic-pro.
  Return nil if a step fails
  Returns true otherwise

  Params:
  * `datomic-root-dir` where to store all datomic versions
  * `datomic-dir-pattern` the directory where datomic will be installed, %s is replaced with the version
  * `datomic-url-pattern` url where to curl the datomic package, %s will be replaced with the version
  * `datomic-ver` version of the datomic archive, (e.g. `1.0.7021`)
  * `datomic-transactor-bin-path` relative path to `datomic-dir` for the transactor binary
  * `force?` when true, the datomic archive is downloaded and installed again, even if already present"
  [datomic-root-dir
   datomic-dir-pattern
   datomic-url-pattern
   datomic-ver
   datomic-transactor-bin-path
   force?]
  (if (str/blank? datomic-ver)
    (build-log/warn "Datomic-ver should be not nil")
    (let [datomic-dir (-> (datomic-dir-setup datomic-root-dir datomic-dir-pattern datomic-ver)
                          build-files/expand-home)
          datomic-download-url (format datomic-url-pattern datomic-ver)
          datomic-archive-file "datomic.zip"
          tmp-dir (build-files/create-temp-dir "datomic")]
      (when force? (build-files/delete-files datomic-dir))
      (if (is-datomic-transactor? datomic-dir datomic-transactor-bin-path)
        (do (build-log/info "Setup skipped, as transactor already installed")
            build-exit-codes/cannot-execute)
        (do (build-log/info-format "Datomic is being downloaded from `%s`" datomic-download-url)
            (when (build-cmds/execute-and-trace ["curl"
                                                 "-SL"
                                                 datomic-download-url
                                                 "-o"
                                                 datomic-archive-file
                                                 {:dir tmp-dir
                                                  :error-to-std? true}]
                                                ["unzip" datomic-archive-file {:dir tmp-dir}])
              (build-log/debug-format "datomic is installing in dir `%s`" datomic-dir)
              (if (zero? (->
                           (build-cmds/execute-with-exit-code
                            ["mv" (format "datomic-pro-%s" datomic-ver) datomic-dir {:dir tmp-dir}])
                           ffirst))
                (do (build-log/info "Datomic downloaded sucessfully!") build-exit-codes/ok)
                (do (build-log/info "Datomic download failed") build-exit-codes/catch-all))))))))

(defn- run-datomic*
  "Starts datomic transactor in the `datomic-dir`.

  Params:
  * `datomic-root-dir` where to store all datomic versions
  * `datomic-dir-pattern` the directory where datomic will be installed, %s is replaced with the version
  * `datomic-transactor-bin-path` relative path to `datomic-dir` for the transactor binary
  * `datomic-ver` version of the datomic archive, (e.g. `1.0.7021`)"
  [datomic-root-dir datomic-dir-pattern datomic-transactor-bin-path datomic-ver]
  (build-cmds/execute-and-trace
   [datomic-transactor-bin-path
    "-Ddatomic.printConnectionInfo=true"
    "config/samples/dev-transactor-template.properties"
    {:dir (datomic-dir-setup datomic-root-dir datomic-dir-pattern datomic-ver)
     :background? true}]))

(defn run
  "Start datomic transactor in directory and ensure it is there.

  Params:
  * `datomic-root-dir` where to store all datomic versions
  * `datomic-dir-pattern` the directory where datomic will be installed, %s is replaced with the version
  * `datomic-transactor-bin-path` relative path to `datomic-dir` for the transactor binary
  * `datomic-ver` version of the datomic archive, (e.g. `1.0.7021`)"
  [datomic-root-dir datomic-dir-pattern datomic-transactor-bin-path datomic-ver]
  (let [datomic-dir (datomic-dir datomic-root-dir datomic-dir-pattern datomic-ver)]
    (if (is-datomic-transactor? datomic-dir datomic-transactor-bin-path)
      (run-datomic* datomic-root-dir datomic-dir-pattern datomic-transactor-bin-path datomic-ver)
      (build-log/warn "Datomic transactor not found, please run storage-install first."))))

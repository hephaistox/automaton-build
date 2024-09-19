(ns automaton-build.project.versioning
  "Code related to app versioning, holds current strategy for versioning"
  (:require
   [automaton-build.code.vcs   :as build-vcs]
   [automaton-build.os.cmds    :as build-commands]
   [automaton-build.os.file    :as build-file]
   [automaton-build.os.version :as build-version]
   [clojure.string             :as str]))

(defn production?
  "Tells if version is a production one or test env"
  [version]
  (nil? (second (build-version/split-optional-qualifier version))))

(defn correct-environment?
  "Checks that the right environment is targeted for deploy according to our practices.
   So version for la should have optional qualifier and for production there should be no qualifier.
   Covers case that project branch version will be different because there was deploy to la and than to  production."
  [app-dir environment]
  (let [current-version-production? (production? (build-version/current-version app-dir))]
    (if (or (and current-version-production? (= :production environment))
            (and (not current-version-production?) (not= :production environment)))
      true
      false)))

(defn version-changed?
  "Checks if current version in `app-dir` is the same as in `target-branch`"
  [app-dir repo target-branch]
  (let [tmp-dir (build-file/create-temp-dir)
        current-app-version (build-version/current-version app-dir)
        clone-res
        (-> (build-vcs/clone-file-chain-cmd repo tmp-dir target-branch (build-version/version-file))
            build-commands/chain-cmds
            build-commands/first-failing)]
    (and (build-commands/success clone-res)
         (not= (build-version/current-version tmp-dir) current-app-version))))

(defn- generate-new-test-env-version
  [version]
  (let [splitted-version (build-version/split-optional-qualifier version)]
    (str/join "-"
              (if (= 2 (count splitted-version))
                [(first splitted-version) 2 (second splitted-version)]
                [(first splitted-version)
                 (str (inc (Integer. (second splitted-version))))
                 (last splitted-version)]))))

(defn- generate-production-version
  "Generates version that is production ready.
   If there is optional qualifier it is stripped and kept
   Else asks user for new version"
  ([version app-name changes]
   (if (production? version)
     (build-version/generate-new-version version app-name changes)
     (first (build-version/split-optional-qualifier version))))
  ([version app-name] (generate-production-version version app-name nil)))

(defn- generate-test-env-version
  "Generates version that is to be used in test environment.
   If there is optional qualifier appends one number to it (e.g. 1.0.0-la -> 1.0.0-2-la)
   else adds optional qualifier based on targeted-env"
  ([version app-name target-env changes]
   (if (production? version)
     (let [new-version (build-version/generate-new-version version app-name changes)]
       (if (= version new-version)
         version
         (build-version/add-optional-qualifier new-version (name target-env))))
     (generate-new-test-env-version version)))
  ([version app-name target-env] (generate-test-env-version version app-name target-env nil)))


(defn generate-new-app-version
  "Based on break versioning.
   Design decision: we update our version based on environment.
   We use optional qualifier for version that is supposed to go to test environment.
   E.g. 1.0.0 for `la` env will have -la qualifier (1.0.0-la) repush of that version will be  1.0.0-2-la.
   Version ready for production don't have optional qualifier."
  [target-env app-dir app-name]
  (if-let [current-version (build-version/current-version app-dir)]
    (if (= :production target-env)
      (generate-production-version current-version app-name)
      (generate-test-env-version current-version app-name target-env))
    {:status :failed
     :message "couldn't get app current version"
     :app-dir app-dir
     :app-name app-name}))

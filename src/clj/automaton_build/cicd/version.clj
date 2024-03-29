(ns automaton-build.cicd.version
  "Version of the current codebase

  * Version is based on a root project file named `version.edn`.
  Strengths:
  * One source of truth for version.
  * Change of version in a PR is more straightforward to notice
  * Flexibility (We don't decide on versioning strategy, we just have file that is point of reference to what version you are on.)
  Constraints:
   * Keeping up with updates for version files (although with automated tooling we have, it is not that big of a deal currently)

  Previous design:
  * major version has to be changed in the major-version in `build_config.edn` (many sources of truth and build_config shouldn't be a place that is used as variable that change often)
  * counting commits from a branch to base a minor version on it (Many edgecases where commits don't match the version, initial commits, PRs having more than one commit...)"
  (:require
   [automaton-build.log             :as build-log]
   [automaton-build.os.cli-input    :as build-cli-input]
   [automaton-build.os.edn-utils    :as build-edn-utils]
   [automaton-build.os.files        :as build-files]
   [automaton-build.os.terminal-msg :as build-terminal-msg]
   [clojure.string                  :as str]))

(defn version-file
  ([] "version.edn")
  ([dir] (build-files/create-file-path dir (version-file))))

(defn- slurp-version-file
  [app-dir]
  (let [version-filename (version-file app-dir)]
    (if (build-files/is-existing-file? version-filename)
      (build-edn-utils/read-edn version-filename)
      (build-log/warn-format "Version file in %s does not exist" app-dir))))

(defn current-version [app-dir] (:version (slurp-version-file app-dir)))

(defn- spit-version-file
  [app-dir content]
  (build-edn-utils/spit-edn
   (version-file app-dir)
   content
   "Last generated version, note a failed push consume a number"))

(defn save-version
  "Update app version file `version.edn` in `app-dir`. Captures requirement for the version to be consciously decided when saved
  Params:
  * `app-dir` directory of the version to count
  * `new-version`"
  [app-dir new-version]
  (spit-version-file app-dir {:version new-version})
  new-version)

(defn ask-version
  "Asks user what should be a new version following the non-breaking version system"
  ([project-name current-version changes]
   (when changes
     (build-terminal-msg/println-msg (format "To see what changed visit %s"
                                             changes)))
   (ask-version project-name current-version))
  ([project-name current-version]
   (build-cli-input/question-loop
    (format
     "Project `%s` current version is: `%s`.\nPattern is <major>.<minor>.<non-breaking>.\nPress \n1 to update major \n2 to update minor \n3 to update non-breaking \n4 Add version manually."
     project-name
     current-version)
    #{"1" "2" "3" "4"})))

(defn ask-manual-version
  "Asks user to input version manually"
  []
  (build-terminal-msg/println-msg
   "What the version should be?\n Remember to follow <major>.<minor>.<non-breaking>[-optional-qualifier] pattern.")
  (flush)
  (build-cli-input/user-input-str))

(defn split-optional-qualifier
  "Removes optional qualifier. (Semantic versioning: <major>.<minor>.<non-breaking>[-optional-qualifier])
   So e.g. `0.0.20-SNAPSHOT` -> `0.0.20`"
  [version]
  (str/split version #"-"))

(defn production?
  "Tells if version is a production one or test env"
  [version]
  (nil? (second (split-optional-qualifier version))))

(defn add-optional-qualifier
  [version qualifier]
  (str/join "-" [version qualifier]))

(defn- inc-str [num] (str (inc (read-string num))))

(defn generate-new-version
  [current-version app-name changes]
  (let [user-version (ask-version app-name current-version changes)
        version-spitted (str/split current-version #"\.")
        major-version (first version-spitted)
        minor-version (second version-spitted)
        non-breaking (last version-spitted)]
    (str/join "."
              (case user-version
                "1" [(inc-str major-version) "0" "0"]
                "2" [major-version (inc-str minor-version) "0"]
                "3" [major-version minor-version (inc-str non-breaking)]
                "4" [(str (ask-manual-version))]))))

(defn- generate-new-test-env-version
  [version]
  (let [splitted-version (split-optional-qualifier version)]
    (str/join "-"
              (if (= 2 (count splitted-version))
                [(first splitted-version) 2 (second splitted-version)]
                [(first splitted-version)
                 (str (inc (Integer. (second splitted-version))))
                 (last splitted-version)]))))

(defn generate-production-version
  "Generates version that is production ready.
   If there is optional qualifier it is stripped and kept
   Else asks user for new version"
  ([version app-name changes]
   (if (production? version)
     (generate-new-version version app-name changes)
     (first (split-optional-qualifier version))))
  ([version app-name] (generate-production-version version app-name nil)))

(defn generate-test-env-version
  "Generates version that is to be used in test environment.
   If there is optional qualifier appends one number to it (e.g. 1.0.0-la -> 1.0.0-2-la)
   else adds optional qualifier based on targeted-env"
  ([version app-name target-env changes]
   (if (production? version)
     (-> (generate-new-version version app-name changes)
         (add-optional-qualifier target-env))
     (generate-new-test-env-version version)))
  ([version app-name target-env]
   (generate-test-env-version version app-name target-env nil)))

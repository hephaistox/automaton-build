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
   [automaton-build.log :as build-log]
   [automaton-build.os.cli-input :as build-cli-input]
   [automaton-build.os.edn-utils :as build-edn-utils]
   [automaton-build.os.files :as build-files]
   [clojure.string :as str]))

(def version-file "version.edn")

(defn read-version-file
  [app-dir]
  (let [version-filename (build-files/create-file-path app-dir version-file)]
    (if (build-files/is-existing-file? version-filename)
      (build-edn-utils/read-edn version-filename)
      (build-log/warn-format "Version file in %s does not exist" app-dir))))

(defn save-version-file
  [app-dir content]
  (build-edn-utils/spit-edn
   (build-files/create-file-path app-dir version-file)
   content
   "Last generated version, note a failed push consume a number"))

(defn current-version [app-dir] (:version (read-version-file app-dir)))

(defn confirm-version?
  "It's safety measure before changing version of the project to be sure user is concious of change."
  [project-name old-version new-version]
  (build-cli-input/yes-question
   (format
    "Your change will affect the version of the project `%s`, old version `%s` replaced with `%s` one are you sure you want to continue? y/n"
    project-name
    old-version
    new-version)))

(defn ask-version
  "It's safety measure before changing version of the project to be sure user is concious of change."
  [project-name current-version changes]
  (build-cli-input/question-loop
   (format
    "Project `%s` current version is: `%s`.\nPattern is <major>.<minor>.<non-breaking>.\nTo see what changed see: `%s`\nPress \n1 to update major \n2 to update minor \n3 to update non-breaking."
    project-name
    current-version
    changes)
   #{1 2 3}))

(defn split-optional-qualifier
  "Removes optional qualifier. (Semantic versioning: <major>.<minor>.<non-breaking>[-optional-qualifier])
   So e.g. `0.0.20-SNAPSHOT` -> `0.0.20`"
  [version]
  (str/split version #"-"))

(defn add-optional-qualifier
  [version qualifier]
  (str/join "-" [version qualifier]))

(defn- inc-str [num] (str (inc (read-string num))))

(defn generate-new-version [])

(defn update-version
  "Build the string of the version to be pushed (the next one)
  Params:
  * `app-dir` directory of the version to count
  * `major-version`"
  ([app-dir app-name changes] (update-version app-dir app-name changes nil))
  ([app-dir app-name changes qualifier]
   (let [current-version (current-version app-dir)
         user-version (ask-version app-name current-version changes)
         version-spitted (str/split current-version #"\.")
         major-version (first version-spitted)
         minor-version (second version-spitted)
         [non-breaking _] (split-optional-qualifier (last version-spitted))
         new-version (str/join
                      "."
                      (case user-version
                        1 [(inc-str major-version) "0" "0"]
                        2 [major-version (inc-str minor-version) "0"]
                        3 [major-version minor-version (inc-str non-breaking)]))
         new-version*
         (if qualifier (str/join "-" [new-version qualifier]) new-version)]
     (if (confirm-version? app-name current-version new-version*)
       (do (save-version-file app-dir {:version new-version*}) new-version*)
       (build-log/warn "Version couldn't be updated without user consent")))))

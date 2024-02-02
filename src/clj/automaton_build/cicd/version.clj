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
  ([force? project-name old-version new-version]
   (build-cli-input/yes-question
    (format
     "Your change will affect the version of the project `%s`, old version `%s` replaced with `%s` one are you sure you want to continue? y/n"
     project-name
     old-version
     new-version)
    force?)))

(defn remove-optional-qualifier
  "Removes optional qualifier. (Semantic versioning: <major>.<minor>.<non-breaking>[-optional-qualifier])
   So e.g. `0.0.20-SNAPSHOT` -> `0.0.20`"
  [version]
  (first (str/split version #"-")))

(defn update-version
  "Build the string of the version to be pushed (the next one)
  Params:
  * `app-dir` directory of the version to count
  * `major-version`"
  [app-dir app-name major-version force?]
  (if major-version
    (let [{_version :version
           older-minor-version :minor-version
           older-major-version :major-version}
          (read-version-file app-dir)
          minor-version
          (if-not (= older-major-version
                     (-> major-version
                         remove-optional-qualifier
                         (format -1)))
            (do (build-log/info "A new major version is detected")
                (build-log/trace-format "Older major version is `%s`"
                                        older-major-version)
                (build-log/trace-format "Newer major version is `%s`"
                                        (format major-version -1))
                -1)
            older-minor-version)
          new-minor-version (inc (or minor-version -1))
          major-version-only (format major-version -1)
          new-version (format major-version new-minor-version)]
      (build-log/trace-format "Major version: %s, old minor: %s, new minor %s"
                              major-version
                              older-minor-version
                              minor-version)
      (if
        (confirm-version? force? app-name (current-version app-dir) new-version)
        (do (save-version-file app-dir
                               {:major-version major-version-only
                                :version new-version
                                :minor-version new-minor-version})
            new-version)
        (build-log/warn "Version couldn't be updated without user consent")))
    (build-log/warn "Major version is missing")))

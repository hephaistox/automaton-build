(ns automaton-build.app.git-push-local-code
  (:require
   [automaton-build.cicd.cfg-mgt :as build-cfg-mgt]
   [automaton-build.log          :as build-log]
   [automaton-build.os.cli-input :as build-cli-input]))

(defn push-current-branch
  "Pushes `app` current changes to it's repository. The changes are pushed to the current branch of user running the function. It is forbidden to push to base-branch of application."
  [app-dir app-name repo main-branch message force?]
  (let [current-branch (build-cfg-mgt/current-branch app-dir)]
    (cond
      (not-every? some? [app-dir repo main-branch current-branch])
      (do (build-log/info-format "App %s is skipped due to missing value"
                                 app-name)
          true)
      (= current-branch main-branch)
      (do
        (build-log/info-format
         "App `%s` is skipped as branch name `%s` is the same as main branch of the app"
         app-name
         current-branch)
        true)
      :else
      (if (or force?
              (build-cli-input/yes-question
               (format
                "Are you sure you want push changes from `%s` to branch `%s`?"
                app-name
                current-branch)))
        (build-cfg-mgt/push-local-dir-to-repo
         (merge {:source-dir app-dir
                 :repo-address repo
                 :force? force?
                 :target-branch current-branch}
                (when message {:commit-msg message})))
        true))))

(defn push-base-branch
  "Pushes app to `base-branch`"
  [app-name app-dir repo base-branch version message]
  (let [github-new-changes-link
        (str "https://github.com/" (first (re-find #"(?<=:)(.*)(?=.git)" repo))
             "/tree/" base-branch)]
    (build-log/info-format
     "Pushing app: `%s` to branch: `%s`. \nChanges can be seen here `%s`"
     app-name
     base-branch
     github-new-changes-link)
    (build-cfg-mgt/push-local-dir-to-repo {:source-dir app-dir
                                           :repo-address repo
                                           :tag {:id version
                                                 :msg message}
                                           :commit-msg message
                                           :force? true
                                           :target-branch base-branch})))

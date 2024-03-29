(ns automaton-build.cicd.deployment
  (:require
   [automaton-build.cicd.cfg-mgt        :as build-cfg-mgt]
   [automaton-build.cicd.clever-cloud   :as build-clever-cloud]
   [automaton-build.cicd.deployment.jar :as build-deploy-jar]
   [automaton-build.configuration       :as build-conf]
   [automaton-build.log                 :as build-log]
   [automaton-build.os.cli-input        :as build-cli-input]
   [automaton-build.os.files            :as build-files]))

(defn publish-library
  "Publish jar to clojars"
  [jar-path pom-path]
  (build-deploy-jar/deploy
   jar-path
   pom-path
   {"clojars" {:url "https://clojars.org/repo"
               :username (build-conf/read-param [:clojars-username])
               :password (build-conf/read-param [:clojars-password])}})
  true)

(defn publish-app
  "Publish uber-jar to Clever Cloud. [clever docs](https://developers.clever-cloud.com/doc/cli/)"
  ([repo-uri target-dir clever-dir version]
   (let [clever-repo-dir
         (build-clever-cloud/clone-repo clever-dir repo-uri "repo")]
     (build-files/copy-files-or-dir [target-dir]
                                    (build-files/create-dir-path clever-repo-dir
                                                                 "target"))
     (build-clever-cloud/deploy clever-repo-dir version)))
  ([repo-uri target-dir]
   (publish-app repo-uri
                target-dir
                (->> ".clever"
                     (build-files/create-dir-path ".")
                     build-files/absolutize)
                "Automatically pushed version")))

(defn push-app-local
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
      :else (if (build-cli-input/yes-question
                 (format
                  "Are you sure you want push changes from `%s` to branch `%s`?"
                  app-name
                  current-branch))
              (let [res (build-cfg-mgt/push-local-dir-to-repo
                         (merge {:source-dir app-dir
                                 :repo-address repo
                                 :force? force?
                                 :target-branch current-branch}
                                (when message {:commit-msg message})))]
                (build-log/debug-format "Pushed %s with result %s" app-name res)
                (-> res
                    last
                    first
                    zero?))
              true))))

(defn push-app-base
  "Pushes app to `base-branch` and updates version file."
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

(ns automaton-build.tasks.container
  "Manages container.
  Proxy to docker technology"
  (:require
   [automaton-build.code.vcs                :as build-vcs]
   [automaton-build.echo.headers            :refer [h1
                                                    h1-error!
                                                    h1-valid
                                                    h1-valid!
                                                    h2
                                                    h2-error
                                                    h2-valid
                                                    h2-valid!
                                                    normalln
                                                    uri-str]]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.os.filename             :as build-filename]
   [automaton-build.project.impl.gh-yml     :as build-gh-yml]
   [automaton-build.project.map             :as build-project-map]
   [automaton-build.tasks.impl.headers.cmds :refer
                                            [blocking-cmd long-living-cmd simple-shell success]]
   [clojure.string                          :as str]))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(def ^:private cli-opts-common-def
  (-> [["-l" "--list" "List existing containers"]
       ["-g" "--build-gha" "Build the gha container"]
       ["-i" "--interactive CONTAINER" "Connect interactively to a container"]
       ["-s" "--stop" "Stop containers"]
       ["-p" "--push" "Push container - skipped without -g option"]
       ["-c" "--clear" "Clear local containers"]
       ["-m" "--clear-image" "Clear local image"]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)))

(def cli-opts-monorepo (build-cli-opts/parse-cli cli-opts-common-def))

(def ^:private verbose? (get-in cli-opts-monorepo [:options :verbose]))

(def ^:private stop? (get-in cli-opts-monorepo [:options :stop]))

(def ^:private docker-cli-name "Name of docker command" "docker")

(defn- docker-connected
  "Returns true if docker is connected, - standard output in `res` doesn't contain Cannot connect...

  Display error message to tell if found / started."
  []
  (let [res (-> (blocking-cmd [docker-cli-name "ps"] "." nil verbose?)
                :err)
        connected? (-> res
                       (str/includes? "Cannot connect to the Docker daemon at")
                       not)
        installed? (-> res
                       (str/includes? "Cannot run program")
                       not)]
    (cond
      (not connected?) (h1-error! "Docker is not started.")
      (not installed?) (h1-error! "Docker is not installed properly."))
    (and installed? connected?)))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(defn container-list
  "List containers"
  [app-dir]
  (h1 "List container images")
  (let [res (blocking-cmd [docker-cli-name "images"] app-dir "Docker images failed" verbose?)]
    (when (success res)
      (h1-valid "List container images")
      (->> res
           :out
           normalln))))

(defn containers-stop
  [app-dir]
  (h1 "Stop containers")
  (let [res (blocking-cmd [docker-cli-name "ps" "-q"]
                          app-dir
                          "Listing running dockers has failed"
                          verbose?)]
    (when (success res)
      (h1-valid "Stop containers")
      (let [container-ids (->> res
                               :out
                               str/split-lines)]
        (if (= [""] container-ids)
          (h2-valid! "No container to stop")
          (doseq [container-id container-ids]
            (h2 "Stop container" container-id)
            (let [res (blocking-cmd [docker-cli-name "stop" container-id]
                                    app-dir
                                    "Unable to remove docker"
                                    verbose?)]
              (if (success res)
                (h2-valid "Container" container-id "is stopped.")
                (h2-error "Container" container-id "cannot stop")))))))))

(defn containers-clear
  [app-dir]
  (h1 "Clear local containers")
  (let [res (blocking-cmd [docker-cli-name "ps" "-a" "-q"]
                          app-dir
                          "Local clear of dockers have failed"
                          verbose?)]
    (when (success res)
      (h1-valid "Clear containers")
      (let [container-ids (->> res
                               :out
                               str/split-lines)]
        (if (= [""] container-ids)
          (h2-valid! "No container to remove")
          (doseq [container-id container-ids]
            (h2 "Remove container" container-id)
            (let [res (blocking-cmd [docker-cli-name "rm" container-id]
                                    app-dir
                                    (str "Impossible to remove container" container-id)
                                    verbose?)]
              (when (success res) (h2-valid "Sucessfully removed container" container-id)))))))))

(defn containers-image
  [app-dir]
  (h1 "Clear local images.")
  (let [res (blocking-cmd [docker-cli-name "image" "ls" "-q"]
                          app-dir
                          "Local list of docker images has failed"
                          verbose?)]
    (when (success res)
      (h1-valid "Clear images")
      (let [image-ids (->> res
                           :out
                           str/split-lines)]
        (if (= [""] image-ids)
          (h2-valid! "No image to remove")
          (doseq [image-id image-ids]
            (h2 "Remove image" image-id)
            (let [res (blocking-cmd [docker-cli-name "rmi" (if stop? "-f" "") image-id]
                                    app-dir
                                    (str "Impossible to remove image" image-id)
                                    verbose?)]
              (when (success res) (h2-valid "Sucessfully removed image" image-id)))))))))


(defn container-interactive
  [app-dir connected-dir container-image-name]
  (let [connected-dir (build-filename/absolutize connected-dir)]
    (h1 "Start interactively container" container-image-name)
    (let [res (simple-shell [docker-cli-name
                             "run"
                             "-p"
                             "8282:8080"
                             "--platform"
                             "linux/amd64"
                             "-it"
                             "--entrypoint"
                             "/bin/bash"
                             "-v"
                             (str connected-dir ":/usr/app")
                             container-image-name]
                            {:dir (build-filename/absolutize app-dir)
                             :continue true})]
      (if (success res)
        (h1-valid! "Connected and has quitted")
        (h1-error! "Connected and quit with an error")))))

(defn container-build
  [app-dir container-image-name force-amd?]
  (h1-valid! "Build container" container-image-name
             "with" (when-not force-amd? "not")
             "force-amd? in dir" (uri-str app-dir))
  (long-living-cmd (concat [docker-cli-name "build"]
                           (when force-amd? ["--platform" "linux/amd64"])
                           ["-t" container-image-name "."])
                   app-dir
                   100
                   verbose?
                   (constantly true)
                   (constantly true)))

(defn container-push
  [container-image-name account version]
  (h1-valid! "Push container" container-image-name "in account" account "for version" version)
  (let [container-hub-uri (format "%s/%s:%s" account container-image-name version)
        res (blocking-cmd [docker-cli-name "tag" container-image-name container-hub-uri]
                          "."
                          "Tagging of the image has failed."
                          verbose?)]
    (when (success res)
      (let [pushing-res (long-living-cmd [docker-cli-name "push" container-hub-uri]
                                         "."
                                         100
                                         verbose?
                                         (constantly true)
                                         (constantly true))]
        (println "pushing-res" pushing-res)
        (when-not (= 0 (:exit pushing-res)) (h1-error! "Failed to push the container"))))
    (format "%s/%s" account container-image-name)))

(defn- update-workflow
  "Update all workflow yml files."
  [monorepo-project-map monorepo workflow container-name version]
  (let [app-dirs (->> (get-in monorepo-project-map
                              [:project-config-filedesc :edn :monorepo monorepo :apps])
                      (mapv :app-dir))]
    (doseq [app-dir (conj app-dirs "..")]
      (h2 "Update container" container-name "in workflow" workflow "to version " version)
      (let [workflow-file (build-gh-yml/workflow-yml app-dir workflow)
            updated-wf (-> workflow-file
                           build-file/read-file
                           (build-gh-yml/update-gha-version container-name version))]
        (if-not (build-file/write-file workflow-file updated-wf)
          (h2-error "Can't write update yaml in" workflow-file)
          (h2-valid "Updated container" container-name
                    "in workflow" workflow
                    "to version " version))
        (when verbose? (normalln "file:" (uri-str workflow-file)))))))

(defn- container-url
  "Returns the url for `container-name`"
  [container-name]
  (format "https://hub.docker.com/repository/docker/%s/general" container-name))

;;*****************************************************************
;; API
;;*****************************************************************
(def container-name "gha-image")
(def container-dir "container_images/gha_image")

(defn run-monorepo
  "Monorepo containers."
  []
  (normalln "Manages containers.")
  (normalln)
  (let [app-dir ""
        monorepo-name :default
        amd-64-built? true
        monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config)
        version (->> (concat [:project-config-filedesc :edn :monorepo]
                             [monorepo-name :gha :version])
                     (get-in monorepo-project-map))
        _ (when-not (docker-connected) (System/exit build-exit-codes/invalid-state))
        list (get-in cli-opts-monorepo [:options :list])
        clear (get-in cli-opts-monorepo [:options :clear])
        clear-image (get-in cli-opts-monorepo [:options :clear-image])
        interactive (get-in cli-opts-monorepo [:options :interactive])
        build-gha (get-in cli-opts-monorepo [:options :build-gha])
        push (get-in cli-opts-monorepo [:options :push])]
    (when list (container-list app-dir))
    (when stop? (containers-stop app-dir))
    (when clear (containers-clear app-dir))
    (when clear-image (containers-image app-dir))
    (when-let [container interactive] (container-interactive app-dir app-dir container))
    (when build-gha
      (when-not (= 0 (:exit (container-build container-dir container-name amd-64-built?)))
        (h1-error! "Failed container build."))
      (when verbose? (normalln "Build container in" (uri-str container-dir))))
    (when push
      (h1 "Check vcs status is clean")
      (if-not (-> (build-vcs/clean-state)
                  (blocking-cmd "." "VCS status check fail" verbose?)
                  build-vcs/clean-state-analyze)
        (do (h1-error! "State not clean to update docker version.")
            (System/exit build-exit-codes/invalid-state))
        (h1-valid "Vcs is clean"))
      (when (nil? version)
        (h1-error! "Tag is missing in [:monorepo monorepo-name :gha :version], pushed is aborted.")
        (System/exit build-exit-codes/invalid-argument))
      (let [container-name (->> version
                                (container-push container-name "hephaistox"))]
        (update-workflow monorepo-project-map
                         monorepo-name
                         "commit_validation"
                         container-name
                         version)
        (h1-valid (format "See publication in %s (version %s)."
                          (->> container-name
                               container-url
                               uri-str)
                          version))))
    (when-not (or list stop? clear clear-image interactive build-gha push)
      (normalln "No action required. Show list of container images.")
      (container-list app-dir))))

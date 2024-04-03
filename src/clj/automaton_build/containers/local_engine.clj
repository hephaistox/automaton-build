(ns automaton-build.containers.local-engine
  "Gather all commands to manage the containers through the local container engine

  Is a docker proxy"
  (:require
   [automaton-build.log         :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.files    :as build-files]
   [clojure.string              :as str]))

(defn container-installed?*
  "Check if docker is properly installed
  Params:
  * `docker-cmd` optional parameter telling the container command"
  ([docker-cmd]
   (if (zero? (-> (build-cmds/execute-with-exit-code [docker-cmd
                                                      "-v"
                                                      {:out :string
                                                       :dir "."}])
                  ffirst))
     true
     (do (build-log/error "Docker is not properly installed") false)))
  ([] (container-installed?* "docker")))

(def container-installed? (memoize container-installed?*))

(defn push-container
  "Push the container named `container-image-name`
  on docker hub account named `account`
  Params:
  * `container-image-name` the name of the image to push, as seen in the container repo
  * `account` account to be used to name the container"
  [container-image-name account]
  (let [container-hub-uri (str/join "/" [account container-image-name])]
    (build-log/debug "Push the container `" container-image-name "`")
    (build-cmds/execute-and-trace
     ["docker" "tag" container-image-name container-hub-uri {:dir "."}]
     ["docker" "push" container-hub-uri {:dir "."}])))

(defn build-container-image
  "Builds the container image
  Params:
  * `container-image-name` the name of the image to build
  * `target-container-dir` is where the Dockerfile should be"
  [container-image-name target-container-dir]
  (build-log/debug-format "Build `%s` docker image in `%s`"
                          container-image-name
                          target-container-dir)
  (when (build-cmds/execute-and-trace ["docker"
                                       "build"
                                       "--platform"
                                       "linux/amd64"
                                       "-t"
                                       container-image-name
                                       "."
                                       {:dir target-container-dir
                                        :error-to-std? true}])
    (build-log/debug-format "Build of `%s` completed" container-image-name)
    true))

(defn container-interactive
  "Creates the container interactive command, for the container named `:container-image-name`
  * `container-image-name` the name of the image to build
  * `container-local-root` is the local directory where the `/usr/app` in the container will be connected"
  [container-image-name container-local-root]
  (build-cmds/execute-and-trace-return-exit-codes
   ["docker"
    "run"
    "--platform"
    "linux/amd64"
    "-p"
    "8282:8080"
    "-it"
    "--entrypoint"
    "/bin/bash"
    "-v"
    (str (build-files/absolutize container-local-root) ":/usr/app")
    container-image-name
    {:dir "."}]))

(defn container-image-list
  "List all locally available images"
  []
  (build-cmds/execute-get-string-and-exit-code ["docker" "images" {:dir "."}]))

(defn container-clean
  "Clean all containers, and images"
  []
  (let [[return-code cmd-res] (-> (build-cmds/execute-get-string-and-exit-code
                                   ["docker" "ps" "-a" "-q" {:dir "."}])
                                  first)
        containers (str/split-lines cmd-res)]
    (if (= 1 return-code)
      (build-log/fatal
       "Impossible to execute docker simple images, is docker running?")
      (do (build-log/trace "containers:" containers)
          (if (= [""] containers)
            (build-log/trace "no container to remove")
            (doseq [container containers]
              (build-log/trace "Remove container id: " container)
              (build-cmds/execute-and-trace
               ["docker" "stop" container {:dir "."}]
               ["docker" "rm" container {:dir "."}])))
          (let [images (-> (build-cmds/execute-get-string
                            ["docker" "images" "-q" {:dir "."}])
                           first
                           str/split-lines)]
            (build-log/trace "images: " images)
            (if (= [""] images)
              (build-log/trace "no image to remove")
              (doseq [image images]
                (build-cmds/execute-and-trace
                 ["docker" "rmi" "--force" image {:dir "."}]))))))))

(defn build-and-push-image
  "Build the container image called `image-to-build`
  A temporary directory is created in `container-target-dir` to gather:
  * the files described in `files`
  * and the content of directory of `container-dir`
  The image is built and pushed to container repository with account `account`,
  only if there are some modifications since the last build on that computer
  Params:
  * `image-to-build` the name of the image to build for container aliases
  * `remote-repo-account` the account to connect to the container remote repository
  * `image-src-dir` directory with the source content of the container
  * `assembled-container-dir` the temporary directory where the assembly of that container is stored
  * `files` is list of other files to pick and add to the container image
  * `publish?` do the publication if true, skip otherwise"
  [image-to-build
   remote-repo-account
   image-src-dir
   assembled-container-dir
   files
   publish?]
  (when (container-installed?)
    (build-log/trace-format
     "Build in `%s` directory and push `%s` to remote repo"
     assembled-container-dir
     image-to-build)
    (let [files (keep build-files/is-existing-file? files)]
      (when (and (build-files/copy-files-or-dir (concat [image-src-dir] files)
                                                assembled-container-dir)
                 (build-container-image image-to-build assembled-container-dir))
        (if publish?
          (push-container image-to-build remote-repo-account)
          true)))))

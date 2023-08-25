(ns automaton-build.adapters.container
  "Gather all commands to manage the containers.
  Is a docker prxoxy"
  (:require
   [clojure.string :as str]

   [automaton-build.adapters.commands :as cmds]
   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]))

(defn container-installed?*
  "Check if docker is properly installed
  Params:
  * `docker-cmd` optional parameter telling the container command"
  ([docker-cmd]
   (try
     (cmds/exec-cmds [[[docker-cmd "-v"]]]
                     {:out :string
                      :dir "."})
     (catch clojure.lang.ExceptionInfo e
       (throw (ex-info (format "Docker is not working, aborting, command `%s` has failed" docker-cmd)
                       {:exception e
                        :docker-cmd docker-cmd})))))
  ([]
   (container-installed?* "docker")))

(def container-installed?
  (memoize container-installed?*))

(defn push-container
  "Push the container named `container-image-name`
  on docker hub account named `account`
  Params:
  * `container-image-name` the name of the image to push, as seen in the container repo
  * `account` account to be used to name the container"
  [container-image-name account]
  (let [container-hub-uri (str/join "/"  [account container-image-name])]
    (log/debug "Push the container `" container-image-name "`")
    (log/trace (cmds/exec-cmds [[["docker" "tag" container-image-name container-hub-uri]]
                                [["docker" "push" container-hub-uri]]]
                               {:dir "."
                                :out :string}))))

(defn build-container-image
  "Builds the container image
  Params:
  * `container-image-name` the name of the image to build
  * `target-container-dir` is where the Dockerfile should be"
  [container-image-name target-container-dir]
  (log/debug "Build `" container-image-name "` docker image")
  (log/trace (cmds/exec-cmds [[["docker" "build" "--platform" "linux/amd64" "-t" container-image-name "."]]]
                             {:dir target-container-dir
                              :out :string}))
  (log/debug "Build of `" container-image-name "` completed"))

(defn container-interactive
  "Creates the container interactive command, for the container named `:container-image-name`
  * `container-image-name` the name of the image to build
  * `container-local-root` is the local directory where the `/usr/app` in the container will be connected"
  [container-image-name container-local-root]
  (cmds/exec-cmds [[["docker" "run" "--platform" "linux/amd64" "-p" "8282:8080" "-it" "--entrypoint" "/bin/bash"
                     "-v" (str (files/absolutize container-local-root)  ":/usr/app") container-image-name]]]
                  {:dir "."}))

(defn container-image-list
  "List all locally available images"
  []
  (cmds/exec-cmds [[["docker" "images"]]]
                  {:out :string
                   :dir "."}))

(defn container-clean
  "Clean all containers, and images"
  []
  (let [containers (str/split-lines (cmds/exec-cmds [[["docker" "ps" "-a" "-q"]]]
                                                    {:out :string
                                                     :dir "."}))]
    (log/trace "containers:" containers)
    (if (= [""] containers)
      (log/trace "no container to remove")
      (doseq [container containers]
        (log/trace "Remove container id: " container)
        (log/trace (cmds/exec-cmds [[["docker" "stop" container]]
                                    [["docker" "rm" container]]]
                                   {:out :string
                                    :dir "."})))))
  (let [images (str/split-lines (cmds/exec-cmds [[["docker" "images" "-q"]]]
                                                {:out :string
                                                 :dir "."}))]
    (log/trace "images: " images)
    (if (= [""] images)
      (log/trace "no image to remove")
      (doseq [image images]
        (log/trace "Remove image id: " image)
        (log/trace (cmds/exec-cmds [[["docker" "rmi" "--force" image]]]
                                   {:out :string
                                    :dir "."}))))))

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
  [image-to-build remote-repo-account image-src-dir assembled-container-dir files publish?]
  (container-installed?)
  (log/trace "Build in `" assembled-container-dir "` directory and push " image-to-build " to remote repo")

  (files/copy-files-or-dir (concat [image-src-dir]
                                   files)
                           assembled-container-dir)
  (build-container-image image-to-build
                         assembled-container-dir)
  (when publish?
    (push-container image-to-build
                    remote-repo-account)))

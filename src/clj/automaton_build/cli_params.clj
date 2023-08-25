(ns automaton-build.cli-params
  "Manages the cli parameters, their validation and translation to tasks parameters"
  (:require
   [clojure.string :as str]

   [automaton-build.adapters.log :as log]
   [automaton-build.app :as app]
   [automaton-build.apps :as apps]
   [automaton-build.container.core :as container-core]))

(defn create-task-params
  "* Validate if the cli parameters meets the expectations of the `mode`, dos an exception if not
  * Returns the map of task parameters which translate first-param and second-param keys"
  ([apps
    cli-params-mode
    {:keys [task-name first-param second-param]
     :as _cli-params}
    quit-fn]
   (let [quit (fn [message]
                (log/error message)
                (log/debug "Cli params are " cli-params-mode ", params are `" first-param "` and `" second-param "`")
                (quit-fn)
                message)]
     (case cli-params-mode
       :none (if (or first-param
                     second-param)
               (quit (format "No parameter is expected Usage: bb %s" task-name))
               {})

       :one-app (cond (or (nil? first-param)
                          (not (string? first-param))
                          (some? second-param))
                      (quit (format "Expect one application Usage `bb %s app` where app is among %s"
                                    task-name
                                    (apps/app-names apps)))
                      :else {:app-name first-param})

       :file-name-and-language (do
                                 (when (nil? first-param)
                                   (quit (format "Usage: bb %s \"file-name\" \"language\" where file-name is a name of the .edn file from docs/customer_materials/" task-name)))
                                 (when (nil? second-param)
                                   (quit (format "Usage: bb %s \"file-name\" \"language\" where language is a language from a file defined in previous param e.g. \"fr\"" task-name)))
                                 {:filename first-param
                                  :language second-param})

       :string (let [str-param first-param]
                 (cond
                   (or (not (string? str-param)) second-param)  (quit (format "One parameter string is expected. Usage: bb %s \"message\", where message is a string" task-name))
                   :else {:str-param str-param}))

       :force? (let [str-param first-param
                     force? (= "force" str-param)]
                 (cond
                   second-param (quit (format "One parameter string is expected. Usage: `bb %s (force)+`, where message is a string" task-name))
                   (not (or (nil? str-param)
                            force?)) (quit (format "The first parameter is force or nothing. Usage: `bb %s (force)+`" task-name))
                   :else {:force? force?}))

       :msg-force? (let [msg first-param
                         force-param second-param
                         force? (= "force" force-param)]
                     (if (str/blank? msg)
                       (quit (format "The first parameter is force or nothing, the second a message telling what user story (ies) you're testing. Usage: `bb %s message (force)+`" task-name))
                       {:force? force?
                        :msg msg}))

       :string-optstring (let [str-param1 first-param
                               str-param2 second-param]
                           (cond
                             (or (not (string? str-param1))
                                 (and
                                  (not (nil? str-param2))
                                  (not (string? str-param2)))) (quit (format "Two parameter string is expected. Usage: bb %s \"message1\" \"message2\", where message is a string" task-name))
                             :else {:str-param1 str-param1
                                    :str-param2 str-param2}))

       :one-app-but-everything (let [app-name first-param
                                     app-but-everything-names (apps/app-but-everything-names apps)]
                                 (cond (or second-param (nil? app-name)) (quit (format "Usage: bb %s [app-name], where app-name is one of `%s`" task-name app-but-everything-names))
                                       (not (apps/is-app-but-everything? apps app-name)) (quit (format "App `%s` is not a known app: `%s` " app-name app-but-everything-names))
                                       :else {:app-name app-name}))

       :one-cust-app (let [cust-app-name first-param
                           cust-app (try
                                      (apps/search-app-by-name apps cust-app-name)
                                      (catch Exception _
                                        nil))]
                       (cond (or second-param
                                 (nil? cust-app-name)) (quit (format "Usage: bb %s [app-name], where app-name is one of `%s`" task-name (apps/cust-app-names apps)))
                             (not (:cust-app? cust-app)) (quit (format "App `%s` is not a known app: %s" cust-app-name (apps/cust-app-names apps)))
                             :else {:cust-app-name cust-app-name}))

       :one-cust-app-with-commit (let [cust-app-name first-param
                                       cust-app (try
                                                  (apps/search-app-by-name apps cust-app-name)
                                                  (catch Exception _
                                                    nil))
                                       commit-msg second-param]
                                   (cond
                                     (nil? cust-app-name) (quit (format "Usage: bb %s [app-name] \"commit string\", where app-name is one of `%s`" task-name (apps/cust-app-names apps)))
                                     (str/blank? commit-msg) (quit (format "Usage: bb %s [%s] \"commit string\", where commit string is a non empty string" task-name cust-app-name))
                                     (not (:cust-app? cust-app)) (quit (format "Cust-app `%s` is not a known app: %s" cust-app-name (apps/cust-app-names apps)))
                                     (not (string? commit-msg)) (quit (format "The second parameter is expected to be a string, we found `%s`" commit-msg))
                                     :else
                                     {:cust-app-name cust-app-name
                                      :commit-msg commit-msg}))

       :one-not-app-names (let [not-app-name first-param]
                            (cond
                              (or second-param (nil? not-app-name)) (quit (format "Usage: bb `%s` [app-name], where app-name is not one of %s" task-name (apps/app-names apps)))
                              (apps/is-app? apps not-app-name) (quit (format "App %s is already defined, please don't use any of those names: %s" not-app-name (apps/app-names apps)))
                              :else
                              {:cust-app-name not-app-name}))

       :one-container-image (let [container-image-name first-param]
                              (cond (or second-param (nil? container-image-name)) (quit (format "Usage: bb %s [local], where local is one of: %s" task-name (container-core/container-image-container-names)))
                                    (not (container-core/is-container-image? container-image-name)) (quit (format "Container image %s is not a known container image: %s " container-image-name (container-core/container-image-container-names)))
                                    :else
                                    {:container-image-name container-image-name}))

       :one-cust-app-or-everything (let [cust-app-or-everything-name first-param
                                         cust-app-or-everything (try
                                                                  (apps/search-app-by-name apps cust-app-or-everything-name)
                                                                  (catch Exception _
                                                                    nil))]
                                     (cond (or second-param (nil? cust-app-or-everything-name)) (quit (format "Cust-app should not be followed by a second parameter, Usage: bb `%s` [app-name|everything], found `%s`" task-name cust-app-or-everything-name))
                                           (not (app/is-cust-app-or-everything? cust-app-or-everything)) (quit (format "App `%s` is not a know cust-app: %s" cust-app-or-everything-name (apps/app-but-everything-names apps)))
                                           :else
                                           {:one-cust-app-or-everything cust-app-or-everything-name}))

       (quit (format "No matching case for %s" cli-params-mode)))))
  ([apps cli-params-mode cli-params]
   (create-task-params apps
                       cli-params-mode
                       cli-params
                       (constantly false))))

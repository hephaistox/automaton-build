(ns automaton-build.apps.templating
  "App creation and update with templates

  Design note:
  After two attempts, the clojure templating libraries have been stopped to be replaced by a simple pattern matching.
  Clostache was first used, and others looked at, but everything found was meant for HTML, with escaping features, limitation on symbols, ...
  Clojure leiningen was based on that also, check https://github.com/day8/re-frame-template/blob/master/src/leiningen/new/re_frame/src/core.cljs for instance, where the namespace does not compile."
  [:require
   [clojure.string :as str]

   [automaton-build.adapters.build-config :as build-config]
   [automaton-core.adapters.deep-merge :as deep-merge]
   [automaton-core.adapters.edn-utils :as edn-utils]
   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-build.adapters.templating :as templating]
   [automaton-core.env-setup :as env-setup]
   [automaton-build.app :as app]
   [automaton-build.apps :as apps]])

(def template-params
  {:prefix-delimiter "chewi-"
   :suffix-delimiter "-hansolo"
   :mark "Template-app manages this namespace"
   :rendered-file-extensions "**{clj,cljs,cljc,edn,md,json,xml,txt,css}"
   :excluded-files #{"node_modules" "package-lock.json" ".shadow-cljs" "target" ".cpcache"}})

(defn published-app-dir
  "Directory where an app is locally stored to publish it outside of the monorepo
  Params:
  * `app-name`: Application name"
  [app-name]
  (files/create-dir-path (get-in env-setup/env-setup
                                 [:published-apps :dir])
                         app-name
                         (get-in env-setup/env-setup
                                 [:published-apps :code-subdir])))

(defn- render-tokens-affecting-file-naming
  "A version of template rendering necessary for `:template-app` and `:template_app` tokens which are a specific case in our templating.
  Indeed, all other tokens are using prefix and suffix, but that two ones are not.

  The files are named with template-app and template_app, so the template-app application is a workable application.
  It means the prefix and suffix delimiters are set to blank string for that specific tokens.
  Params:
  * `files` files, vector of filename and content
  * `cust-app-dir` the value replacing token `:template_app`
  * `cust-app-name` the value replacing the token `:template-app`"
  [files cust-app-dir cust-app-name]
  (log/debug "Rename `template-app`= `" cust-app-name "` and `template_app`= `" cust-app-dir "` in the files ")
  (log/trace "files: " (edn-utils/spit-in-tmp-file files))
  (templating/render files
                     {:template-app cust-app-name
                      :template_app cust-app-dir}
                     "" ""))

(defn new-project
  "Creates a new project based on the template-app.
  Params:
  * `template-app-dir` is the directory containing the template application
  * `cust-app-name` should be the name of a non existing cust-app, otherwise an exception is thrown"
  [template-app-dir cust-app-name]
  (let [{:keys [prefix-delimiter suffix-delimiter rendered-file-extensions]} template-params
        cust-app-dir (files/file-ized cust-app-name)
        target-bb-config-filename (files/absolutize (files/create-file-path cust-app-dir
                                                                            (build-config/build-config-filename)))]
    (when (files/directory-exists? cust-app-dir)
      (throw (ex-info "This target directory already exists, init aborted, please clear it or choose a different name"
                      {:cust-app-dir cust-app-dir})))

    (files/copy-files-or-dir [template-app-dir]
                             cust-app-dir)

    (let [files-map (files/create-files-map cust-app-dir rendered-file-extensions)
          tokens-used-in-template (-> (templating/search-tokens files-map
                                                                prefix-delimiter
                                                                suffix-delimiter)
                                      (dissoc
                                       :template-app))]
      (render-tokens-affecting-file-naming files-map cust-app-dir cust-app-name)
      (log/debug "Update " target-bb-config-filename " to create `:templating` map")
      ;; It is important to create a fresh list of files, as it is outdated just after the update
      (edn-utils/update-edn-content target-bb-config-filename
                                    #(deep-merge/deep-merge {:templating tokens-used-in-template
                                                             :cust-app? true}
                                                            %)
                                    (str ";;This file has been initiated during `bb new-project " cust-app-name "`, update the value and then launch `bb refresh-project " cust-app-name "` ")))

    (log/debug "Rename files and directories in `" cust-app-dir "` with that new name")
    (files/rename-recursively cust-app-dir "**" template-app-dir cust-app-dir)

    (log/info "Check `" target-bb-config-filename "` and launch `bb refresh-project `" cust-app-name "`")))

(defn refresh-project
  "Refresh an application based on a template
  Parameters are:
  * `cust-app` is the app to refresh
  * `template-app` where the template app is stored,
  * `simulation?` (Optional, default=false) when true, the destination is not replaced, only a temporary directory

  The files marked as refreshed in the template-app are copied in cust-app
  Then all that files are re-rendered with new build configuration file
  Returns true if a refresh has been done"
  ([cust-app template-app]
   (refresh-project cust-app template-app false))
  ([cust-app template-app simulation?]
   (let [{:keys [prefix-delimiter suffix-delimiter rendered-file-extensions excluded-files mark]} template-params
         {{cust-app-dir :app-dir} :monorepo
          cust-app-templating-data :templating
          cust-app-name :app-name}             cust-app
         template-app-dir (:app-dir template-app)
         tmp-dir (published-app-dir cust-app-name)
         mandatory-data [cust-app-dir prefix-delimiter suffix-delimiter rendered-file-extensions excluded-files mark cust-app-templating-data template-app-dir]
         cust-app-templating-data* (assoc cust-app-templating-data
                                          :app-title cust-app-name)]
     (log/info "Update cust-app `" cust-app-name "` with template-app from `" template-app-dir "`")
     (log/trace "Created in " tmp-dir)
     (files/delete-files [tmp-dir])
     (if (some (comp str/blank? str) mandatory-data)
       (do
         (log/warn "Application `" cust-app-name "` template refresh is skipped, data are missing " mandatory-data)
         false)
       (do
         (files/copy-files-or-dir [template-app-dir]
                                  tmp-dir)
         (doseq [excluded-file excluded-files]
           (files/delete-files (files/search-files tmp-dir excluded-file)))

         (let [tmp-file-map (files/create-files-map tmp-dir rendered-file-extensions)]
           (files/delete-files-starting-with tmp-file-map
                                             (re-pattern mark)))
         (let [tmp-file-map (files/create-files-map tmp-dir rendered-file-extensions)]
           (render-tokens-affecting-file-naming tmp-file-map cust-app-dir cust-app-name))
           ;; It is important to create a new file-map, as the files have been rendered, and so modified below
         (let [tmp-file-map (files/create-files-map tmp-dir rendered-file-extensions)]
           (log/debug "Render the content with application data " (edn-utils/spit-in-tmp-file cust-app-templating-data*))
           (templating/render tmp-file-map
                              cust-app-templating-data*
                              prefix-delimiter
                              suffix-delimiter))

         (files/rename-recursively tmp-dir "**"
                                   template-app-dir cust-app-dir)
         (when-not simulation?
           (files/copy-files-or-dir [tmp-dir]
                                    cust-app-dir))
         true)))))

(defn change-markers
  " Change the marker of a file
  Params:
  * `dir` where the directories are searched for"
  [dir]
  (let [{:keys [rendered-file-extensions mark]} template-params]
    (doseq [[filename file-content] (files/create-files-map dir rendered-file-extensions)]
      (when-let [updated-content (templating/change-marker file-content
                                                           (re-pattern mark)
                                                           "Template-app manages this namespace")]
        (files/spit-file filename updated-content)))))

(defn search-pattern
  "Search for the pattern `searched` in the directories of `apps`
  Only directories set up in deps.edn or shadow-cljs are used
  Params:
  * `apps` applications
  * `searched` is a second filter applied on that files"
  [apps searched]
  (->> apps
       apps/code-filenames-in-apps
       (filter #(re-find (re-pattern searched) %))
       vec))

(defn rename-dirs
  "Rename the directories in `dir`
  Params:
  * `apps` applications
  * `renamings` is a map associated to an app name a sequence of renaming.
  Each renaming is a map
  {:file [\"a\" \"b\"]
  :ns [\"c\" \"d\"]}
  Transforming file a in b and namespace c to d"
  [apps renamings]
  (let [{:keys [rendered-file-extensions]} template-params]
    (doseq [{:keys [app-dir app-name] :as app} apps]
      (when-let [renaming (get renamings app-name)]
        (doseq [{:keys [file _ns] :as _change} renaming]
          (templating/rename-dirs (->> app
                                       app/search-in-codefiles
                                       (filter #(re-find (re-pattern (first file)) %))
                                       vec)
                                  (first file)
                                  (second file)))
        (templating/render (files/create-files-map app-dir
                                                   rendered-file-extensions)
                           (into {}
                                 (map :ns renaming))
                           "" "")))))

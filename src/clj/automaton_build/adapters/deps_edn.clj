(ns automaton-build.adapters.deps-edn
  "Proxy to `deps.edn` file"
  (:require
   [automaton-build.adapters.code-formatter :as code-formatter]
   [automaton-build.adapters.edn-utils :as edn-utils]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.log :as log]))

(def deps-edn
  "deps.edn")

(defn get-deps-filename
  "Get the deps-file of the application
  Params:
  * `app-dir` is where the application is stored"
  [app-dir]
  (files/create-file-path app-dir
                          deps-edn))

(defn spit-deps-edn
  "Spit the `content` in `deps.edn` file
  Params:
  * `app-dir` where to spit the deps.edn file'
  * `content` what to write in the file
  * `header` (optional) header is automatically preceded with ;;
  Returns the content of the file"
  [app-dir content header]
  (let [deps-edn-filename (get-deps-filename app-dir)]
    (log/trace "Write `" (files/absolutize deps-edn-filename) "`")
    (files/create-dirs app-dir)
    (let [content (edn-utils/spit-edn deps-edn-filename
                                      content
                                      (or header
                                          "Modify application directly, touched at "))]
      (code-formatter/format-file deps-edn-filename)
      content)))

(defn load-deps-edn
  "Load the deps.edn file of the app, passed as a parameter,
  Params:
  * `app-dir` the directory of the app, where `deps.edn` is stored
  Returns nil if the file does not exists or is malformed"
  [app-dir]
  (edn-utils/read-edn-or-nil (get-deps-filename app-dir)))

(defn update-deps-edn
  "Update the `deps.edn` file from `app-name`
  Params:
  * `app-dir` is where `deps.edn` file should be updated
  * `update-edn-fn` is the function to update the content, is used with `(update-edn-fn content)`"
  [app-dir update-edn-fn]
  (let [deps-edn-filename (get-deps-filename app-dir)
        _ (log/debug "Update deps file : " (files/absolutize deps-edn-filename))
        original-deps-edn (edn-utils/read-edn deps-edn-filename)
        original-deps-edn (when (map? original-deps-edn)
                            original-deps-edn)
        updated-deps-edn (when original-deps-edn
                           (update-edn-fn original-deps-edn))]
    (cond (= original-deps-edn
             updated-deps-edn) (log/warn "Update skipped, values are identical")
          (not (map? original-deps-edn)) (log/warn "Update skipped, deps.edn is not a map")
          (nil? update-deps-edn) (log/trace "Update aborded, updated content is empty")
          :else (do
                  (log/trace "Update new `" deps-edn-filename "`")
                  (edn-utils/spit-edn deps-edn-filename
                                      updated-deps-edn)
                  (code-formatter/format-file deps-edn-filename
                                        ;; `deps.edn` is a particular case, they are hard coded to be marked
                                        "Template-app manages this namespace\n;; This file is automatically updated by `build.adapters.deps-edn/updated-deps-edn`, at time ")))))

(defn update-commit-id
  "Update the `deps-edn` with the `commit-id` for the dependency `as-lib`
  Params:
  * `as-lib` is the symbol for the library to update
  * `commit-id` is the sha of the commit
  * `deps-edn` is the content of the file"
  [as-lib commit-id deps-edn]
  (if-let [old-commit-id (get-in deps-edn [:deps as-lib :git/sha])]
    (if (= commit-id
           old-commit-id)
      (do
        (log/trace "Skip update as it already uptodate")
        deps-edn)
      (do
        (log/trace "Change commit from `" old-commit-id "` to `" commit-id "`")
        (assoc-in deps-edn
                  [:deps as-lib :git/sha]
                  commit-id)))
    (do
      (log/trace "Skip as it does not use lib `%s:%s`")
      deps-edn)))

(defn extract-paths
  "Extracts the paths from a given `deps.edn`
   e.g. {:run {...}}
  Params:
  * `deps-edn` content the deps edn file to search extract path in
  * `aliases` is a collection of aliases to exclude"
  ([{:keys [paths aliases]
     :as _deps-edn} exclude-aliases]
   (let [selected-aliases (apply dissoc aliases
                                 exclude-aliases)
         alias-paths (mapcat (fn [[_alias-name paths]]
                               (apply concat
                                      (vals (select-keys paths [:extra-paths :paths]))))
                             selected-aliases)]
     (->> alias-paths
          (concat paths)
          sort
          dedupe
          (into []))))
  ([deps-edn]
   (extract-paths deps-edn #{})))

(defn extract-deps
  "Extract dependencies in a `deps.edn` file
  Params:
  * `deps-edn` is the content of the file to search dependencies in"
  [{:keys [deps aliases] :as _deps-edn}]
  (map (fn [[deps-name deps-map]]
         [deps-name deps-map])
       (concat deps
               (into {}
                     (apply concat
                            (map (fn [[_ alias-defs]]
                                   (vals
                                    (select-keys alias-defs [:extra-deps :deps])))
                                 aliases))))))

(defn remove-deps
  "Remove the dependency called `dep-lib-to-remove` in the `deps`
  * `deps-edn` the deps edn content to update
  * `dep-libs-to-remove` is the list of dependencies to remove"
  [deps-edn dep-libs-to-remove]
  (update deps-edn
          :deps
          #(apply dissoc %
                  dep-libs-to-remove)))

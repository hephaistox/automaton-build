(ns automaton-build.headers.vcs
  (:require
   [automaton-build.code.vcs     :as build-vcs]
   [automaton-build.echo.headers :refer [errorln h2 h2-error h2-error! h2-valid uri-str]]
   [automaton-build.os.file      :as build-file]
   [clojure.string               :as str]))

;; (defn remote-branches
;;   "Returns remote branches in `repo-url`."
;;   [repo-url verbose]
;;   (let [target-dir (build-file/create-temp-dir "headers-vcs-test")]
;;     (-> (build-vcs/list-remote-branch-cmd repo-url)
;;         (force-dirs target-dir)
;;         (chain-cmds "Remote branch has failed." verbose)
;;         build-vcs/list-remote-branch-analyze)))

;; (defn remote-branch-exists?
;;   "Is the `local-branch` exists on the remote repository at `repo-url."
;;   [remote-branches local-branch]
;;   (build-vcs/remote-branch-exists? remote-branches local-branch))

;; (defn clone-repo-branch
;;   "`repo-url` in the directory `target-dir`, if provided clones to specific `branch`"
;;   ([target-dir repo-url verbose]
;;    (let [s (new java.io.StringWriter)
;;          res (binding [*out* s]
;;                (-> (build-vcs/shallow-clone-repo-branch-cmd repo-url)
;;                    (blocking-cmd target-dir "Impossible to clone." verbose)))]
;;      (if (success res)
;;        (when verbose (h2-valid "Cloning is successfull."))
;;        (h2-error "Cloning is not successfull."))
;;      (let [s (str s)] (when-not (str/blank? s) (print s)))
;;      (success res)))
;;   ([target-dir repo-url branch verbose]
;;    (if-not (str/blank? branch)
;;      (let [s (new java.io.StringWriter)
;;            _ (when verbose (h2 "Clone repo with branch" (uri-str branch)))
;;            res (binding [*out* s]
;;                  (-> (build-vcs/shallow-clone-repo-branch-cmd repo-url branch)
;;                      (blocking-cmd target-dir "Impossible to clone." verbose)))]
;;        (if (success res)
;;          (when verbose (h2-valid "Branch" (uri-str branch) "cloning is successfull."))
;;          (h2-error "Branch" (uri-str branch) "cloning is not successfull."))
;;        (let [s (str s)] (when-not (str/blank? s) (print s)))
;;        (success res))
;;      (h2-error! "No branch provided."))))

;; (defn create-empty-branch
;;   "Download repo at `repo-url`, and creates `branch` based on latest commit of base-branch,

;;   Which content is completly removed."
;;   [repo-dir repo-url branch base-branch verbose]
;;   (if (str/blank? repo-url)
;;     (do (errorln "Unexpectedly empty `repo-url`") nil)
;;     (do (clone-repo-branch repo-dir repo-url base-branch verbose)
;;         (new-branch-and-switch repo-dir branch verbose)
;;         (->> (build-file/search-files repo-dir "*" {:hidden true})
;;              (remove #(or (str/ends-with? % ".git/") (str/ends-with? % ".git")))
;;              (mapv build-file/delete-path))
;;         repo-dir)))

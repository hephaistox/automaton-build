(ns automaton-build.tasks.code-stats
  (:require
   [automaton-build.code.files   :as build-code-files]
   [automaton-build.echo.headers :refer [h1-error! h1-valid! normalln]]
   [automaton-build.os.cli-opts  :as build-cli-opts]
   [automaton-build.os.file      :as build-file]
   [automaton-build.project.map  :as build-project-map]
   [clojure.string               :as str]))


(defn count-lines
  "Count lines in the file list"
  ([files reader] (count-lines files reader build-file/is-existing-file?))
  ([files reader check-existence-fn]
   (let [files (map str files)
         files-only (filter check-existence-fn files)]
     (reduce +
             (map (fn [file]
                    (let [content (reader file)]
                      (-> content
                          str/split-lines
                          count)))
                  files-only)))))

(defn clj-line-numbers
  "Build statistics on the clojure code
  Params:
  * `all-files` all files to count
  * `test-files` subgroup of all-files with test aliases
  :clj-test-nb-lines : is the total number of lines in test
  :clj-total-nb-lines: total number of lines, clj(c,s) test and not test
  :ratio-in-pct: ratio between both.
     - 100% means there are the same number of test lines than src lines
     - 50% means there are double number of lines src than test ones"
  [all-files test-files]
  (let [clj-total-nb-lines (count-lines all-files slurp)
        clj-test-nb-lines (count-lines test-files slurp)
        clj-src-nb-lines (- clj-total-nb-lines clj-test-nb-lines)]
    {:clj-test-nb-lines clj-test-nb-lines
     :clj-total-nb-lines clj-total-nb-lines
     :ratio-in-pct (if (zero? clj-src-nb-lines)
                     0
                     (/ (Math/floor (* 10000 (/ clj-test-nb-lines clj-src-nb-lines))) 100))}))

(defn line-numbers->str
  [line-numbers]
  (str "# That statistics counts number of line of code\n"
       (str/join "\n" (sort (mapv (fn [[k v]] (str "* " (name k) " - " v)) line-numbers)))))

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(defn run
  [filepath]
  (normalln "Start code statistics report generation to: " filepath)
  (try (let [{:keys [dir edn]} (-> (build-project-map/create-project-map "")
                                   build-project-map/add-deps-edn
                                   :deps)
             clj-code-files (-> (build-code-files/project-dirs dir edn)
                                (build-code-files/project-files "**{.clj,.cljc,.cljs}"))
             test-files (filter #(str/includes? % "test") clj-code-files)
             line-numbers (clj-line-numbers clj-code-files test-files)
             statistics-content (line-numbers->str line-numbers)]
         (normalln statistics-content)
         (build-file/write-file filepath statistics-content)
         (h1-valid! "Code statistics saved to " filepath))
       (catch Exception e (h1-error! "Code statistics failed with message: " e))))

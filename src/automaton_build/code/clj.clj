(ns automaton-build.code.clj
  "A proxy to `clojure.tools.build.api` library for building artifacts in Clojure projects."
  (:require
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.filename :as build-filename]
   [clojure.tools.build.api     :as clj-build-api]))

;; ********************************************************************************
;; Proxy to `clojure.tools.build.api`

(defn create-basis
  "Create a basis from a set of deps sources and aliases."
  ([] (clj-build-api/create-basis))
  ([params] (clj-build-api/create-basis params)))

(defn write-pom
  "Create pom file to META-INF/maven/<groupId>/<artifactId>/pom.xml"
  [params]
  (clj-build-api/write-pom params))

(defn compile-clj
  "Compile Clojure source to classes in :class-dir. Clojure source files are found in :basis :paths by default, or override with :src-dirs."
  [params]
  (merge (try (clj-build-api/compile-clj params)
              {:params params}
              (catch Exception e (throw (ex-info "Compilation failed" {:params params} e))))))

(defn set-project-root!
  "Sets project root variable that's defaulted to \".\" to `root`"
  [root]
  (clj-build-api/set-project-root! root))

(defn jar
  "Create jar file containing contents of class-dir. Use main in the manifest if provided. Returns nil."
  [params]
  (clj-build-api/jar params))

(defn uber
  "Create uberjar file. An uberjar is a self-contained jar file containing
  both the project contents AND the contents of all dependencies. Which makes it runnable with java -jar command"
  [params]
  (clj-build-api/uber params))

;; ********************************************************************************
;; Copy project files

(defn copy-project-files
  "In `project-dir`, copy all files in each `app-paths` in `target-dir`."
  [project-dir app-paths target-dir]
  (->> app-paths
       (mapv #(-> (build-filename/create-dir-path project-dir %)
                  (build-file/copy-action project-dir target-dir)
                  build-file/do-copy-action))))

;; ********************************************************************************
;; Jar compilation

(defn- compile-and-uber-jar
  [project-dir app-paths target-jar-filepath printers verbose jar-main skip-uberjar?]
  (let [{:keys [normalln errorln]} printers
        tmp-dir (build-file/create-temp-dir)]
    (merge
     {:target-dir tmp-dir
      :app-paths app-paths
      :target-jar-filepath target-jar-filepath
      :project-dir project-dir}
     (try (-> (str "Copy files and dirs `" app-paths "` from `" project-dir "` to `" tmp-dir "`")
              normalln)
          (->> (copy-project-files project-dir app-paths tmp-dir)
               (mapv (fn [{:keys [status apath target-path exception]}]
                       (if (= status :success)
                         (when verbose
                           (normalln (str "Copied successfully `" apath "` to `" target-path "`")))
                         (do (errorln (str "Error during copy of `" apath "` to `" target-path "`"))
                             (normalln (pr-str exception)))))))
          (normalln "Compiling")
          (let [basis (create-basis)]
            (println (with-out-str (compile-clj {:basis basis
                                                 :class-dir tmp-dir})))
            (normalln (str "Set project root to `" project-dir "`"))
            (set-project-root! (build-filename/absolutize project-dir))
            (normalln "Create jar")
            (jar {:class-dir tmp-dir
                  :main jar-main
                  :jar-file target-jar-filepath})
            (if skip-uberjar?
              (normalln "Skip uberjar")
              (do (normalln "Create uberjar")
                  (uber {:uber-file target-jar-filepath
                         :class-dir tmp-dir
                         :basis basis
                         :main jar-main}))))
          (normalln "Compilation has succeeded")
          {:status :success}
          (catch Exception e
            (errorln "Compilation failed")
            {:status :failed
             :exception e})))))

(defn compile-jar
  "In a `project-dir`, do a clj compilation with all files and directories from `app-paths`. Stores the resulted jar in `target-jar-filepath`."
  [project-dir app-paths target-jar-filepath printers verbose jar-main]
  (compile-and-uber-jar project-dir app-paths target-jar-filepath printers verbose jar-main true))

(defn compile-uberjar
  "In a `project-dir`, do a clj compilation with all files and directories from `app-paths`. Stores the resulted jar in `target-jar-filepath`."
  [project-dir app-paths target-jar-filepath printers verbose jar-main]
  (compile-and-uber-jar project-dir app-paths target-jar-filepath printers verbose jar-main false))

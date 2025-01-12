(ns automaton-build.code.clj
  "A proxy to `clojure.tools.build.api`library for building artifacts in Clojure projects."
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
  (clj-build-api/compile-clj params))

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

(defn compile-jar
  "In a `project-dir`, do a clj compilation with all files and directories from `app-paths`. Stores the resulted jar in `target-jar-filepath`."
  [project-dir app-paths target-jar-filepath normalln errorln verbose]
  (let [tmp-dir (build-file/create-temp-dir)]
    (merge {:target-dir tmp-dir
            :app-paths app-paths
            :target-jar-filepath target-jar-filepath
            :project-dir project-dir}
           (try
             (normalln
              (str "Copy files and dirs `" app-paths "` from `" project-dir "` to `" tmp-dir "`"))
             (let [copy-ress (copy-project-files project-dir app-paths tmp-dir)]
               (println "copy-ress " (pr-str copy-ress))
               (->> copy-ress
                    (mapv
                     (fn [{:keys [status apath target-path exception]}]
                       (if (= status :success)
                         (when verbose
                           (normalln (str "Copied successfully `" apath "` to `" target-path "`")))
                         (do (errorln (str "Error during copy of `" apath "` to `" target-path "`"))
                             (normalln (pr-str exception))))))))
             #_(comment
                 (normalln "Compiling")
                 (let [basis (create-basis)]
                   (compile-clj {:basis basis
                                 :class-dir tmp-dir})
                   (normalln "Set project root to" project-dir)
                   (set-project-root! (build-filename/absolutize project-dir))
                   (normalln "Create jar")
                   (jar {:class-dir tmp-dir
                         :jar-file target-jar-filepath})))
             {:status :success}
             (catch Exception e
               {:status :failed
                :exception e})))))

(defn uber-jar
  ""
  [class-dir app-paths target-jar-filepath project-dir jar-main java-opts]
  (let [basis (create-basis)]
    (compile-clj {:basis basis
                  :class-dir class-dir
                  :java-opts java-opts})
    (uber {:class-dir class-dir
           :uber-file target-jar-filepath
           :basis basis
           :main jar-main}))
  ;
)
